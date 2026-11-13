package com.example.v2rayng.data

object SecretObfuscator {
    private const val KEY: Byte = 0x5D

    private val encHost = byteArrayOf(
        29, 25, 25, 21, 22, 103, 114, 114, 16, 28, 30, 44, 40, 17, 44, 112, 26, 18, 115, 23, 24, 103, 111, 109, 108, 115
    )

    fun getFixedHost(): String {
        val decrypted = ByteArray(encHost.size)
        for (i in encHost.indices) {
            decrypted[i] = (encHost[i].toInt() xor KEY.toInt()).toByte()
        }
        return String(decrypted, Charsets.UTF_8)
    }
}
