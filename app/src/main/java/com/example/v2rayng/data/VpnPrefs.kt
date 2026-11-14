package com.example.v2rayng.data

import android.content.Context
import android.content.SharedPreferences

class VpnPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("v2rayng_prefs", Context.MODE_PRIVATE)

    val fixedHost: String = SecretObfuscator.getFixedHost()

    var accounts: List<String>
        get() {
            val accsStr = prefs.getString("accounts_list", "") ?: ""
            return accsStr.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it != "test" }
        }
        set(value) {
            val cleanList = value.map { it.trim() }.filter { it.isNotBlank() && it != "test" }
            prefs.edit().putString("accounts_list", cleanList.joinToString(",")).apply()
        }

    var currentUsername: String
        get() {
            val savedUser = prefs.getString("current_username", "") ?: ""
            if (savedUser == "test" || (savedUser.isNotBlank() && savedUser !in accounts)) {
                return accounts.firstOrNull() ?: ""
            }
            return savedUser.ifBlank { accounts.firstOrNull() ?: "" }
        }
        set(value) = prefs.edit().putString("current_username", value).apply()

    fun getSubscription(username: String): String {
        if (username.isBlank() || username == "test") return ""
        return prefs.getString("sub_$username", "") ?: ""
    }

    fun saveSubscription(username: String, content: String) {
        if (username.isNotBlank() && username != "test") {
            prefs.edit().putString("sub_$username", content).apply()
        }
    }

    fun removeSubscription(username: String) {
        prefs.edit().remove("sub_$username").apply()
    }

    var themeMode: String // "Dark", "Light", "System"
        get() = prefs.getString("theme_mode", "Dark") ?: "Dark"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var bypassLan: Boolean
        get() = prefs.getBoolean("bypass_lan", true)
        set(value) = prefs.edit().putBoolean("bypass_lan", value).apply()

    var bypassRu: Boolean
        get() = prefs.getBoolean("bypass_ru", true)
        set(value) = prefs.edit().putBoolean("bypass_ru", value).apply()

    var authToken: String
        get() = prefs.getString("auth_token", "") ?: ""
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    var paymentEmail: String
        get() = prefs.getString("payment_email", "") ?: ""
        set(value) = prefs.edit().putString("payment_email", value).apply()

    val isLoggedIn: Boolean
        get() = authToken.isNotBlank() || currentUsername.isNotBlank()

    fun clearAuth() {
        prefs.edit().remove("auth_token").remove("user_email").apply()
    }
}
