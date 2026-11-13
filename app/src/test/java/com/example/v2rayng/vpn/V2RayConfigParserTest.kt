package com.example.v2rayng.vpn

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2RayConfigParserTest {
    private val link = "vless://00112233-4455-6677-8899-aabbccddeeff@example.com:443" +
        "?security=reality&pbk=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
        "&sid=aabb&sni=www.example.com&fp=chrome&type=tcp&flow=xtls-rprx-vision"

    @Test fun realityParametersReachCoreAndTunIsEnabled() {
        val config = JSONObject(V2RayConfigParser.createConfig(V2RayConfigParser.parse(link), bypassLan = false, bypassRu = false))
        val outbound = config.getJSONArray("outbounds").getJSONObject(0)
        val stream = outbound.getJSONObject("streamSettings")
        assertEquals("reality", stream.getString("security"))
        assertEquals("aabb", stream.getJSONObject("realitySettings").getString("shortId"))
        assertEquals("www.example.com", stream.getJSONObject("realitySettings").getString("serverName"))
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            stream.getJSONObject("realitySettings").getString("publicKey"))
        assertEquals("xtls-rprx-vision", outbound.getJSONObject("settings").getJSONArray("vnext")
            .getJSONObject(0).getJSONArray("users").getJSONObject(0).getString("flow"))
        assertEquals("tun", config.getJSONArray("inbounds").getJSONObject(0).getString("protocol"))
        assertEquals(0, config.getJSONObject("routing").getJSONArray("rules").length())
    }

    @Test fun percentEncodingIsDecodedOnceAndPlusIsPreserved() {
        val node = V2RayConfigParser.parse(link + "&spx=%2Fa%252Fb%2Bc#My%20server")
        assertEquals("/a%2Fb+c", node.spiderX)
        assertEquals("My server", node.name)
    }

    @Test fun ipv6HostAndEmptyShortIdAreSupported() {
        val node = V2RayConfigParser.parse(link.replace("example.com:443", "[2001:db8::1]:8443").replace("sid=aabb", "sid="))
        assertEquals("2001:db8::1", node.host)
        assertEquals(8443, node.port)
        assertEquals("", node.shortId)
    }

    @Test fun bypassLanUsesExplicitRoutesWithoutGeoAssets() {
        val config = JSONObject(V2RayConfigParser.createConfig(V2RayConfigParser.parse(link), bypassLan = true, bypassRu = false))
        val rule = config.getJSONObject("routing").getJSONArray("rules").getJSONObject(0)
        assertEquals("direct", rule.getString("outboundTag"))
        assertTrue(rule.getJSONArray("ip").toString().contains("192.168.0.0/16"))
    }

    @Test fun bypassRuAddsRuDomainsToDirectRouting() {
        val config = JSONObject(V2RayConfigParser.createConfig(V2RayConfigParser.parse(link), bypassLan = false, bypassRu = true))
        val rules = config.getJSONObject("routing").getJSONArray("rules")
        assertEquals(1, rules.length())
        val rule = rules.getJSONObject(0)
        assertEquals("direct", rule.getString("outboundTag"))
        assertTrue(rule.getJSONArray("domain").toString().contains("domain:ru"))
        assertTrue(rule.getJSONArray("domain").toString().contains("domain:gosuslugi.ru"))
    }

    @Test fun grpcServiceNameReachesCore() {
        val node = V2RayConfigParser.parse(link.replace("type=tcp", "type=grpc")
            .replace("flow=xtls-rprx-vision", "serviceName=my%2Fservice"))
        val config = JSONObject(V2RayConfigParser.createConfig(node, false))
        assertEquals("my/service", config.getJSONArray("outbounds").getJSONObject(0)
            .getJSONObject("streamSettings").getJSONObject("grpcSettings").getString("serviceName"))
    }

    @Test fun xhttpParametersReachCore() {
        val xhttpLink = "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:29640" +
            "?encryption=none&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D" +
            "&fp=firefox&host=&mode=auto&path=%2Fapi%2Fv1&pbk=h3XE_ZlnOe5j5ZfWvRzKtAvS2mJMn7zc0i1jKmRlbUM" +
            "&security=reality&sid=23d67b2a&sni=www.24teatro.ru&spx=%2Fcb0cf787e94cde6&type=xhttp" +
            "&x_padding_bytes=100-1000#Автовыбор-test|⚡37"
        val node = V2RayConfigParser.parse(xhttpLink)
        assertEquals("xhttp", node.network)
        assertEquals("/api/v1", node.path)
        assertEquals("auto", node.mode)
        assertEquals("Автовыбор-test|⚡37", node.name)

        val config = JSONObject(V2RayConfigParser.createConfig(node, false))
        val outbound = config.getJSONArray("outbounds").getJSONObject(0)
        val stream = outbound.getJSONObject("streamSettings")
        assertEquals("xhttp", stream.getString("network"))
        assertEquals("reality", stream.getString("security"))
        val xhttpSettings = stream.getJSONObject("xhttpSettings")
        assertEquals("/api/v1", xhttpSettings.getString("path"))
        assertEquals("auto", xhttpSettings.getString("mode"))
        val extra = xhttpSettings.getJSONObject("extra")
        assertEquals("auto", extra.getString("mode"))
        assertEquals("100-1000", extra.getString("xPaddingBytes"))
    }

    @Test fun subscriptionNodesAreAllValidAndParsable() {
        val subscriptionLinks = listOf(
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:29640?encryption=none&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D&fp=firefox&host=&mode=auto&path=%2Fapi%2Fv1&pbk=h3XE_ZlnOe5j5ZfWvRzKtAvS2mJMn7zc0i1jKmRlbUM&security=reality&sid=23d67b2a&sni=www.24teatro.ru&spx=%2Fcb0cf787e94cde6&support-x25519mlkem768=true&type=xhttp&x_padding_bytes=100-1000#Автовыбор-test|⚡37",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:30674?encryption=none&flow=xtls-rprx-vision&fp=firefox&pbk=CHamnIPuCDdS889S_PRYEVRVEN80b6B0JISee3wmahI&security=reality&sid=a27a09cf81&sni=staticview.microsoft.com&spx=%2F19c3836b2e7925c&support-x25519mlkem768=true&type=tcp# 🇩🇪 vless",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:27838?encryption=none&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D&fp=firefox&host=&mode=auto&path=%2F&pbk=Aat3N2nHfYNbD_x0eCkZqk30xHcsvq2DqQt3E5bSHVU&security=reality&sid=40155233b97a573e&sni=connect-sit.amd.com&spx=%2Fa3ec44e2fc307a2&support-x25519mlkem768=true&type=xhttp&x_padding_bytes=100-1000# 🇩🇪 xhttp",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:27120?authority=&encryption=none&fp=firefox&pbk=dEAfg8EkNDvL28XM-l0T60LyglBQu-2RJRMflF3U1BM&security=reality&serviceName=&sid=ca&sni=support.amd.com&spx=%2F9d432049d8789ef&support-x25519mlkem768=true&type=grpc# 🇩🇪 gRPC",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:35280?encryption=none&flow=xtls-rprx-vision&fp=firefox&pbk=7TdvOS9RztzFc5m53ZHou506VeGBNT6iHA4REh3qN2A&security=reality&sid=11eb9c7b7ad4&sni=pro.radeon.com&spx=%2F8bb3a3f7d00f419&support-x25519mlkem768=true&type=tcp#🇩🇪 vless",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:46420?encryption=none&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D&fp=firefox&host=&mode=auto&path=%2F&pbk=tbJYyCxWal2eDODDSPQoED9MfcQe__NgIVj3BwtQZ1w&security=reality&sid=6b5d0edfdd&sni=privacy.microsoft.com&spx=%2Ff5dc79e6d2710be&support-x25519mlkem768=true&type=xhttp&x_padding_bytes=100-1000#🇩🇪 xhttp",
            "vless://78c402f9-0f50-4850-9a94-1258819d4f9e@mikaela-go.ru:25833?authority=&encryption=none&fp=firefox&pbk=8pyEjOIggJwTZ3qW0_lVtPyMoGEH1N1VeKYwMO5cWVQ&security=reality&sid=fd9f488e85f7cd30&sni=shield.nvidia.asia&spx=%2Fc31c21b98cd67a8&support-x25519mlkem768=true&type=grpc#🇩🇪 gRPC"
        )

        for (linkItem in subscriptionLinks) {
            val node = V2RayConfigParser.parse(linkItem)
            val jsonConfig = V2RayConfigParser.createConfig(node, bypassLan = false, bypassRu = false)
            val configObj = JSONObject(jsonConfig)
            assertTrue(configObj.has("outbounds"))
        }
    }

    @Test fun invalidConfigurationsAreRejectedInsteadOfSilentlyConnecting() {
        listOf(
            link.replace("00112233-4455-6677-8899-aabbccddeeff", "bad-uuid"),
            link.replace("security=reality", "security=tls"),
            link.replace("sid=aabb", "sid=xyz"),
            link.replace("type=tcp", "type=ws"),
            link.replace("type=tcp", "type=grpc"),
            link.replace("pbk=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "pbk="),
            link.replace("sni=www.example.com", "sni=")
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { V2RayConfigParser.parse(invalid) }
        }
    }
}
