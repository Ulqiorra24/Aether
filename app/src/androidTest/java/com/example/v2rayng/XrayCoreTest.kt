package com.example.v2rayng

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.v2rayng.vpn.V2RayConfigParser
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket

/** Loads the actual native library and validates REALITY config without contacting a real server. */
@RunWith(AndroidJUnit4::class)
class XrayCoreTest {
    @Test fun nativeCoreAcceptsRealityAndRestarts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Seq.setContext(context)
        Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
        val node = V2RayConfigParser.parse(
            "vless://00112233-4455-6677-8899-aabbccddeeff@127.0.0.1:44443" +
                "?security=reality&sni=example.com&pbk=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "&sid=aabb&fp=chrome&type=tcp&flow=xtls-rprx-vision"
        )
        val config = JSONObject(V2RayConfigParser.createConfig(node, false))
        // TUN requires user consent; this smoke test isolates native loading and configuration.
        val socks = config.getJSONArray("inbounds").getJSONObject(1).put("port", 11080)
        config.put("inbounds", JSONArray().put(socks))
        val core = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long = 0
            override fun shutdown(): Long = 0
            override fun onEmitStatus(code: Long, message: String?): Long = 0
        })
        repeat(2) {
            try {
                core.startLoop(config.toString(), 0)
                assertTrue(core.isRunning)
                Socket("127.0.0.1", 11080).use { socket ->
                    socket.soTimeout = 3000
                    socket.getOutputStream().write(byteArrayOf(5, 1, 0))
                    assertTrue(socket.getInputStream().read() == 5)
                    assertTrue(socket.getInputStream().read() == 0)
                }
            } finally {
                core.stopLoop()
            }
            assertFalse(core.isRunning)
        }
    }
}
