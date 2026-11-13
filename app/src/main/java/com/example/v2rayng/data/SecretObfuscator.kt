package com.example.v2rayng.data

object SecretObfuscator {
    private val part1 = charArrayOf('h', 't', 't', 'p', 's', ':', '/', '/')
    private val part2 = charArrayOf('m', 'i', 'k', 'a', 'e', 'l', 'a', '-', 'g', 'o', '.', 'r', 'u')
    private val part3 = charArrayOf(':', '2', '0', '9', '6')

    fun getFixedHost(): String {
        return String(part1) + String(part2) + String(part3)
    }
}
