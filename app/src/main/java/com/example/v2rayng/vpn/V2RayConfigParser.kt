package com.example.v2rayng.vpn

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

data class ParsedNode(
    val host: String,
    val port: Int,
    val id: String,
    val serverName: String,
    val publicKey: String,
    val shortId: String,
    val fingerprint: String,
    val flow: String,
    val network: String,
    val path: String,
    val serviceName: String,
    val spiderX: String,
    val name: String,
    val mode: String = "",
    val hostHeader: String = "",
    val authority: String = "",
    val extraJson: String = "",
    val xPaddingBytes: String = ""
)

object V2RayConfigParser {
    const val SOCKS_PORT = 10808

    fun parse(uriString: String): ParsedNode {
        val trimmed = uriString.trim()
        val hashIndex = trimmed.indexOf('#')
        val mainPart = if (hashIndex != -1) trimmed.substring(0, hashIndex) else trimmed
        val rawFragment = if (hashIndex != -1) trimmed.substring(hashIndex + 1) else ""

        val uri = try { URI(mainPart) } catch (_: Exception) {
            throw IllegalArgumentException("Некорректная ссылка сервера")
        }
        require(uri.scheme.equals("vless", true)) { "Поддерживается VLESS + REALITY" }
        val host = uri.host?.removeSurrounding("[", "]")
        require(!host.isNullOrBlank()) { "Не указан адрес сервера" }
        val port = if (uri.port == -1) 443 else uri.port
        require(port in 1..65535) { "Некорректный порт сервера" }
        val id = decode(uri.rawUserInfo.orEmpty())
        require(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(id)) {
            "Некорректный UUID VLESS"
        }
        UUID.fromString(id)
        val params = uri.rawQuery.orEmpty().split("&").filter { it.isNotEmpty() }.associate {
            val parts = it.split("=", limit = 2)
            decode(parts[0]) to decode(parts.getOrElse(1) { "" })
        }
        require(params["security"] == "reality") { "Поддерживается VLESS с security=reality" }
        val serverName = params["sni"].orEmpty()
        require(serverName.isNotBlank()) { "В ссылке отсутствует SNI (sni)" }
        val publicKey = params["pbk"].orEmpty()
        require(Regex("[A-Za-z0-9_-]{43}").matches(publicKey)) { "Некорректный публичный ключ REALITY (pbk)" }
        val shortId = params["sid"].orEmpty()
        require(shortId.length <= 16 && shortId.length % 2 == 0 && shortId.all { it in "0123456789abcdefABCDEF" }) {
            "Некорректный short ID REALITY (sid)"
        }
        val network = params["type"].orEmpty().ifEmpty { "tcp" }.let { if (it == "raw") "tcp" else it }
        require(network in setOf("tcp", "grpc", "xhttp")) { "Транспорт $network пока не поддерживается" }
        require(params["headerType"].orEmpty() in setOf("", "none")) { "TCP headerType не поддерживается" }
        require(params["encryption"].orEmpty() in setOf("", "none")) { "Поддерживается encryption=none" }
        val flow = params["flow"].orEmpty()
        require(flow in setOf("", "xtls-rprx-vision")) { "Неподдерживаемый flow" }
        require(flow.isEmpty() || network == "tcp") { "Vision требует транспорт TCP" }
        val serviceName = (params["serviceName"] ?: params["service_name"]).orEmpty()
        val authority = (params["authority"] ?: params["host"]).orEmpty()
        val mode = params["mode"].orEmpty()
        val hostHeader = params["host"].orEmpty()
        val extraJson = params["extra"].orEmpty()
        val xPaddingBytes = (params["x_padding_bytes"] ?: params["xPaddingBytes"]).orEmpty()

        return ParsedNode(
            host, port, id, serverName, publicKey, shortId,
            params["fp"].orEmpty().ifEmpty { "chrome" }, flow, network,
            params["path"].orEmpty(), serviceName,
            params["spx"].orEmpty(), decode(rawFragment).ifBlank { host },
            mode = mode,
            hostHeader = hostHeader,
            authority = authority,
            extraJson = extraJson,
            xPaddingBytes = xPaddingBytes
        )
    }

    fun createConfig(node: ParsedNode, bypassLan: Boolean = true, bypassRu: Boolean = true): String {
        val reality = JSONObject()
            .put("serverName", node.serverName)
            .put("fingerprint", node.fingerprint)
            .put("publicKey", node.publicKey)
            .put("shortId", node.shortId)
            .put("spiderX", node.spiderX)
        val stream = JSONObject().put("network", node.network).put("security", "reality")
            .put("realitySettings", reality)
        if (node.network == "grpc") {
            val grpcObj = JSONObject().put("serviceName", node.serviceName)
            if (node.authority.isNotBlank()) {
                grpcObj.put("authority", node.authority)
            }
            stream.put("grpcSettings", grpcObj)
        } else if (node.network == "xhttp") {
            val xhttpObj = JSONObject()
            if (node.hostHeader.isNotBlank()) {
                xhttpObj.put("host", node.hostHeader)
            }
            xhttpObj.put("path", node.path.ifEmpty { "/" })
            xhttpObj.put("mode", node.mode.ifEmpty { "auto" })

            val extraObj = if (node.extraJson.isNotBlank()) {
                try { JSONObject(node.extraJson) } catch (_: Exception) { JSONObject() }
            } else {
                JSONObject()
            }
            if (!extraObj.has("mode") && node.mode.isNotBlank()) {
                extraObj.put("mode", node.mode)
            }
            if (!extraObj.has("xPaddingBytes") && node.xPaddingBytes.isNotBlank()) {
                extraObj.put("xPaddingBytes", node.xPaddingBytes)
            }
            if (extraObj.length() > 0) {
                xhttpObj.put("extra", extraObj)
            }

            stream.put("xhttpSettings", xhttpObj)
        }
        val user = JSONObject().put("id", node.id).put("encryption", "none").put("flow", node.flow)
        val server = JSONObject().put("address", node.host).put("port", node.port)
            .put("users", JSONArray().put(user))
        val proxy = JSONObject().put("tag", "proxy").put("protocol", "vless")
            .put("settings", JSONObject().put("vnext", JSONArray().put(server)))
            .put("streamSettings", stream)
        val rules = JSONArray()
        if (bypassLan) {
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("outboundTag", "direct")
                    .put("ip", JSONArray(listOf(
                        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
                        "127.0.0.0/8", "169.254.0.0/16", "::1/128", "fc00::/7", "fe80::/10"
                    )))
                    .put("domain", JSONArray(listOf(
                        "regexp:^.*\\.local$", "regexp:^.*\\.lan$", "domain:local"
                    )))
            )
        }
        if (bypassRu) {
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("outboundTag", "direct")
                    .put("domain", JSONArray(listOf(
                        "domain:ru",
                        "domain:xn--p1ai",
                        "domain:su",
                        "domain:by",
                        "domain:vk.com",
                        "domain:vk.me",
                        "domain:vk.ru",
                        "domain:yandex.ru",
                        "domain:yandex.com",
                        "domain:yandex.net",
                        "domain:ya.ru",
                        "domain:dzen.ru",
                        "domain:mail.ru",
                        "domain:ok.ru",
                        "domain:rambler.ru",
                        "domain:ozon.ru",
                        "domain:wildberries.ru",
                        "domain:avito.ru",
                        "domain:sberbank.ru",
                        "domain:sber.ru",
                        "domain:tbank.ru",
                        "domain:tinkoff.ru",
                        "domain:vtb.ru",
                        "domain:alfabank.ru",
                        "domain:gosuslugi.ru",
                        "domain:mos.ru",
                        "domain:nalog.ru",
                        "domain:kremlin.ru",
                        "domain:gov.ru",
                        "domain:rutube.ru",
                        "domain:kinopoisk.ru",
                        "domain:hh.ru",
                        "domain:2gis.ru"
                    )))
            )
        }
        return JSONObject()
            .put("log", JSONObject().put("loglevel", "info"))
            .put("inbounds", JSONArray()
                .put(JSONObject().put("tag", "tun-in").put("protocol", "tun")
                    .put("settings", JSONObject().put("name", "aether-tun").put("MTU", 1500)))
                .put(JSONObject().put("tag", "socks-in").put("listen", "127.0.0.1")
                    .put("port", SOCKS_PORT).put("protocol", "socks")
                    .put("settings", JSONObject().put("auth", "noauth").put("udp", true))))
            .put("outbounds", JSONArray().put(proxy)
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom")))
            .put("routing", JSONObject().put("domainStrategy", "AsIs").put("rules", rules))
            .toString()
    }

    private fun decode(value: String): String = URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
}
