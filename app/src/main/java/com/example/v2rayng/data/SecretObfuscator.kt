package com.example.v2rayng.data

object SecretObfuscator {
    private val part1 = charArrayOf('h', 't', 't', 'p', 's', ':', '/', '/')
    private val part2 = charArrayOf('m', 'i', 'k', 'a', 'e', 'l', 'a', '-', 'g', 'o', '.', 'r', 'u')

    fun getFixedHost(): String {
        return String(part1) + String(part2)
    }
}
