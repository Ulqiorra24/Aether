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

    fun createConfig(
        node: ParsedNode,
        bypassLan: Boolean = true,
        bypassRu: Boolean = true,
        hasGeoAssets: Boolean = true
    ): String {
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
            val lanIps = mutableListOf(
                "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
                "127.0.0.0/8", "169.254.0.0/16", "::1/128", "fc00::/7", "fe80::/10"
            )
            if (hasGeoAssets) {
                lanIps.add("geoip:private")
            }
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("outboundTag", "direct")
                    .put("ip", JSONArray(lanIps))
                    .put("domain", JSONArray(listOf(
                        "regexp:^.*\\.local$", "regexp:^.*\\.lan$", "domain:local"
                    )))
            )
        }
        if (bypassRu) {
            val ruDomains = mutableListOf(
                "domain:ru",
                "domain:xn--p1ai",
                "domain:su",
                "domain:by",
                "domain:kz",
                // Application Update Servers (Direct Local Download outside VPN)
                "domain:github.com",
                "domain:githubusercontent.com",
                "domain:objects.githubusercontent.com",
                "domain:mikaela-go.ru",
                "domain:app.mikaela-go.ru",
                // Wildberries
                "domain:wb.ru",
                "domain:wbbasket.ru",
                "domain:wbstatic.net",
                "domain:wildberries.ru",
                "domain:wildberries.com",
                "domain:wildberries.by",
                "domain:wildberries.kz",
                // Ozon
                "domain:ozon.ru",
                "domain:ozonusercontent.com",
                "domain:ozone.ru",
                "domain:ozon-st.ru",
                "domain:ozon.by",
                "domain:ozon.kz",
                "domain:ozon.com",
                "domain:ozonru.me",
                "domain:pvz-ozon.ru",
                // 1C & Bitrix24
                "domain:1c.ru",
                "domain:1c.com",
                "domain:1c-bitrix.ru",
                "domain:1c-bitrix.com",
                "domain:1c-cloud.ru",
                "domain:1cfresh.com",
                "domain:bitrix24.ru",
                "domain:bitrix24.com",
                "domain:bitrix24.net",
                "domain:bitrix24.by",
                "domain:bitrix24.kz",
                "domain:bitrix24.site",
                "domain:bitrix24.tech",
                "domain:bitrix24cdn.com",
                "domain:bitrix24public.com",
                "domain:bitrix.info",
                "domain:bitrix.ru",
                "domain:b24.ru",
                "domain:b24.top",
                "domain:b24.site",
                "domain:sbis.ru",
                "domain:tensor.ru",
                "domain:kontur.ru",
                // VK Ecosystem & Mail.ru
                "domain:vk.com",
                "domain:vk.me",
                "domain:vk.ru",
                "domain:userapi.com",
                "domain:vkuser.net",
                "domain:vkuserlive.net",
                "domain:vkuservideo.net",
                "domain:vkmessenger.com",
                "domain:vk.link",
                "domain:vkplay.ru",
                "domain:vkvideo.ru",
                "domain:vkconnect.ru",
                "domain:vkontakte.ru",
                "domain:mail.ru",
                "domain:imgsmail.ru",
                "domain:ok.ru",
                "domain:odnoklassniki.ru",
                "domain:rambler.ru",
                // Gosuslugi & Government & State
                "domain:gosuslugi.ru",
                "domain:gu-st.ru",
                "domain:gosuslugi.org",
                "domain:esia.gosuslugi.ru",
                "domain:pos.gosuslugi.ru",
                "domain:mos.ru",
                "domain:spb.ru",
                "domain:nalog.ru",
                "domain:nalog.gov.ru",
                "domain:cbr.ru",
                "domain:rzd.ru",
                "domain:kremlin.ru",
                "domain:gov.ru",
                "domain:zakupki.gov.ru",
                "domain:pfr.gov.ru",
                "domain:sfr.gov.ru",
                "domain:fss.ru",
                "domain:mvd.ru",
                "domain:mvd.gov.ru",
                "domain:sudrf.ru",
                "domain:pochta.ru",
                // Yandex Ecosystem
                "domain:yandex.ru",
                "domain:ya.ru",
                "domain:yandex.com",
                "domain:yandex.net",
                "domain:yastatic.net",
                "domain:yango.com",
                "domain:dzen.ru",
                "domain:kinopoisk.ru",
                "domain:yandex.by",
                "domain:yandex.kz",
                // Classifieds & Maps
                "domain:avito.ru",
                "domain:avito.st",
                "domain:hh.ru",
                "domain:2gis.ru",
                "domain:2gis.com",
                // Banks & Finance
                "domain:sberbank.ru",
                "domain:sber.ru",
                "domain:sberbank.com",
                "domain:sber.me",
                "domain:sberbank-ast.ru",
                "domain:sberbusiness.ru",
                "domain:sberprime.ru",
                "domain:sberbank-online.ru",
                "domain:tbank.ru",
                "domain:tinkoff.ru",
                "domain:t-bank.ru",
                "domain:t-static.ru",
                "domain:tinkoff.com",
                "domain:vtb.ru",
                "domain:vtb.com",
                "domain:vtbonline.ru",
                "domain:vtbconnect.ru",
                "domain:alfabank.ru",
                "domain:alfa-bank.ru",
                "domain:alfabank.com",
                "domain:alfastrakhovanie.ru",
                "domain:raiffeisen.ru",
                "domain:raiffeisen.com",
                "domain:gazprombank.ru",
                "domain:sovcombank.ru",
                "domain:open.ru",
                "domain:rosbank.ru",
                "domain:psbank.ru",
                "domain:pochtabank.ru",
                "domain:nspk.ru",
                "domain:sbp.ru",
                "domain:mir-pay.ru",
                "domain:mironline.ru",
                // Mobile & Internet Telecom Operators
                "domain:mts.ru",
                "domain:mts.by",
                "domain:mgts.ru",
                "domain:mts.me",
                "domain:tele2.ru",
                "domain:t2.ru",
                "domain:rt.ru",
                "domain:rostelecom.ru",
                "domain:megafon.ru",
                "domain:beeline.ru",
                "domain:yota.ru",
                "domain:dom.ru",
                "domain:ertelecom.ru",
                "domain:ttk.ru",
                // Retail & Supermarkets & Delivery
                "domain:dns-shop.ru",
                "domain:mvideo.ru",
                "domain:eldorado.ru",
                "domain:megamarket.ru",
                "domain:sbermegamarket.ru",
                "domain:samokat.ru",
                "domain:cdek.ru",
                "domain:lenta.com",
                "domain:5ka.ru",
                "domain:x5.ru",
                "domain:magnit.ru",
                "domain:magnit.tech",
                "domain:rutube.ru",
                // Restaurants & Fast Food
                "domain:vkusnoitochka.ru",
                "domain:dodopizza.ru",
                "domain:dodopizza.com",
                "domain:kuper.ru",
                "domain:sbermarket.ru",
                "domain:burgerkingrus.ru",
                "domain:rostiks.ru",
                "domain:mikafood.ru"
            )
            if (hasGeoAssets) {
                ruDomains.add(0, "geosite:category-ru")
                ruDomains.add(1, "geosite:category-gov-ru")
                ruDomains.add(2, "geosite:category-bank-ru")
                ruDomains.add(3, "geosite:category-ecommerce-ru")
                ruDomains.add(4, "geosite:category-retail-ru")
                ruDomains.add(5, "geosite:category-media-ru")
                ruDomains.add(6, "geosite:tld-ru")
                ruDomains.add(7, "geosite:bitrix")
                ruDomains.add(8, "geosite:vk")
                ruDomains.add(9, "geosite:yandex")
                ruDomains.add(10, "geosite:mailru")
            }
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("outboundTag", "direct")
                    .put("domain", JSONArray(ruDomains))
            )
            if (hasGeoAssets) {
                rules.put(
                    JSONObject()
                        .put("type", "field")
                        .put("outboundTag", "direct")
                        .put("ip", JSONArray(listOf("geoip:ru")))
                )
            }
        }

        val sniffing = JSONObject()
            .put("enabled", true)
            .put("destOverride", JSONArray(listOf("http", "tls", "quic")))
            .put("routeOnly", false)

        val tunInbound = JSONObject()
            .put("tag", "tun-in")
            .put("protocol", "tun")
            .put("settings", JSONObject().put("name", "aether-tun").put("MTU", 9000)) // Use 9000 for better TCP throughput over Xray
            .put("sniffing", sniffing)

        val socksInbound = JSONObject()
            .put("tag", "socks-in")
            .put("listen", "127.0.0.1")
            .put("port", SOCKS_PORT)
            .put("protocol", "socks")
            .put("settings", JSONObject().put("auth", "noauth").put("udp", true))

        return JSONObject()
            .put("log", JSONObject()
                .put("loglevel", "warning")
                .put("access", "")
                .put("error", "")
            )
            .put("inbounds", JSONArray().put(tunInbound).put(socksInbound))
            .put("outbounds", JSONArray().put(proxy)
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom")))
            .put("routing", JSONObject()
                .put("domainStrategy", "IPIfNonMatch")
                .put("rules", rules)
            )
            .toString()
    }

    private fun decode(value: String): String = URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
}
