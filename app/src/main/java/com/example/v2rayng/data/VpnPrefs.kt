package com.example.v2rayng.data

import android.content.Context
import android.content.SharedPreferences

class VpnPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("v2rayng_prefs", Context.MODE_PRIVATE)

    val fixedHost: String = SecretObfuscator.getFixedHost()

    private val ignoredAccounts = setOf("test", "qg9xa3obc1ban6o5")

    init {
        val token = prefs.getString("auth_token", "") ?: ""
        val savedUser = prefs.getString("current_username", "") ?: ""
        val savedEmail = prefs.getString("user_email", "") ?: ""
        val accsStr = prefs.getString("accounts_list", "") ?: ""
        val cleanAccounts = accsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in ignoredAccounts }

        val editor = prefs.edit()
        var changed = false
        if (token.isBlank()) {
            if (savedUser.isNotBlank()) {
                editor.remove("current_username")
                changed = true
            }
            if (savedEmail.isNotBlank()) {
                editor.remove("user_email")
                changed = true
            }
            if (accsStr.isNotBlank()) {
                editor.remove("accounts_list")
                changed = true
            }
        } else {
            if (savedUser in ignoredAccounts) {
                editor.putString("current_username", cleanAccounts.firstOrNull() ?: "")
                changed = true
            }
            if (savedEmail in ignoredAccounts) {
                editor.remove("user_email")
                changed = true
            }
            if (accsStr.split(",").any { it.trim() in ignoredAccounts }) {
                editor.putString("accounts_list", cleanAccounts.joinToString(","))
                changed = true
            }
        }
        if (changed) {
            editor.apply()
        }
    }

    var lastReadNewsId: Int
        get() = prefs.getInt("last_read_news_id", 0)
        set(value) = prefs.edit().putInt("last_read_news_id", value).apply()

    var lastReadSupportMsgId: Int
        get() = prefs.getInt("last_read_support_msg_id", 0)
        set(value) = prefs.edit().putInt("last_read_support_msg_id", value).apply()

    var lastNotifiedSupportMsgId: Int
        get() = prefs.getInt("last_notified_support_msg_id", 0)
        set(value) = prefs.edit().putInt("last_notified_support_msg_id", value).apply()

    var accounts: List<String>
        get() {
            if (authToken.isBlank()) return emptyList()
            val accsStr = prefs.getString("accounts_list", "") ?: ""
            return accsStr.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it !in ignoredAccounts }
        }
        set(value) {
            val cleanList = value.map { it.trim() }.filter { it.isNotBlank() && it !in ignoredAccounts }
            prefs.edit().putString("accounts_list", cleanList.joinToString(",")).apply()
        }

    var currentUsername: String
        get() {
            if (authToken.isBlank()) return ""
            val savedUser = prefs.getString("current_username", "") ?: ""
            if (savedUser in ignoredAccounts || (savedUser.isNotBlank() && savedUser !in accounts)) {
                return accounts.firstOrNull() ?: ""
            }
            return savedUser.ifBlank { accounts.firstOrNull() ?: "" }
        }
        set(value) = prefs.edit().putString("current_username", value).apply()

    fun getSubscription(username: String): String {
        if (username.isBlank() || username in ignoredAccounts) return ""
        return prefs.getString("sub_$username", "") ?: ""
    }

    fun saveSubscription(username: String, content: String) {
        if (username.isNotBlank() && username !in ignoredAccounts) {
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

    var killSwitchEnabled: Boolean
        get() = prefs.getBoolean("kill_switch_enabled", true)
        set(value) = prefs.edit().putBoolean("kill_switch_enabled", value).apply()

    var dnsProvider: String // "AdGuard", "Cloudflare", "Google", "Quad9", "System"
        get() = prefs.getString("dns_provider", "AdGuard") ?: "AdGuard"
        set(value) = prefs.edit().putString("dns_provider", value).apply()

    fun getDnsServers(): List<String> = when (dnsProvider) {
        "AdGuard" -> listOf("94.140.14.14", "94.140.15.15")
        "Cloudflare" -> listOf("1.1.1.1", "1.0.0.1")
        "Google" -> listOf("8.8.8.8", "8.8.4.4")
        "Quad9" -> listOf("9.9.9.9", "149.112.112.112")
        else -> listOf("1.1.1.1", "8.8.8.8")
    }

    var showProtocols: Boolean
        get() = prefs.getBoolean("show_protocols", false)
        set(value) = prefs.edit().putBoolean("show_protocols", value).apply()

    var isAutoSelect: Boolean
        get() = prefs.getBoolean("is_auto_select", false)
        set(value) = prefs.edit().putBoolean("is_auto_select", value).apply()

    var perAppMode: String // "OFF", "ALLOW_SELECTED", "DISALLOW_SELECTED"
        get() = prefs.getString("per_app_mode", "OFF") ?: "OFF"
        set(value) = prefs.edit().putString("per_app_mode", value).apply()

    var perAppAllowPackages: Set<String>
        get() {
            val set = prefs.getStringSet("per_app_allow_packages", null)
            if (set != null) return set
            if (prefs.getString("per_app_mode", "OFF") == "ALLOW_SELECTED") {
                return prefs.getStringSet("per_app_packages", emptySet()) ?: emptySet()
            }
            return emptySet()
        }
        set(value) = prefs.edit().putStringSet("per_app_allow_packages", value).apply()

    var perAppDisallowPackages: Set<String>
        get() {
            val set = prefs.getStringSet("per_app_disallow_packages", null)
            if (set != null) return set
            if (prefs.getString("per_app_mode", "OFF") == "DISALLOW_SELECTED") {
                return prefs.getStringSet("per_app_packages", emptySet()) ?: emptySet()
            }
            return emptySet()
        }
        set(value) = prefs.edit().putStringSet("per_app_disallow_packages", value).apply()

    val perAppPackages: Set<String>
        get() = when (perAppMode) {
            "ALLOW_SELECTED" -> perAppAllowPackages
            "DISALLOW_SELECTED" -> perAppDisallowPackages
            else -> emptySet()
        }

    var authToken: String
        get() = prefs.getString("auth_token", "") ?: ""
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var userEmail: String
        get() {
            if (authToken.isBlank()) return ""
            val email = prefs.getString("user_email", "") ?: ""
            return if (email in ignoredAccounts) "" else email
        }
        set(value) = prefs.edit().putString("user_email", value).apply()

    var paymentEmail: String
        get() {
            if (authToken.isBlank()) return ""
            return prefs.getString("payment_email", "") ?: ""
        }
        set(value) = prefs.edit().putString("payment_email", value).apply()

    val isLoggedIn: Boolean
        get() = authToken.isNotBlank() && currentUsername.isNotBlank() && currentUsername !in ignoredAccounts

    fun isTrialClaimed(username: String): Boolean {
        if (username.isBlank() || username in ignoredAccounts) return true
        return prefs.getBoolean("trial_claimed_$username", false)
    }

    fun setTrialClaimed(username: String, claimed: Boolean = true) {
        if (username.isNotBlank() && username !in ignoredAccounts) {
            prefs.edit().putBoolean("trial_claimed_$username", claimed).apply()
        }
    }

    fun getTrialExpiryTimestamp(username: String): Long {
        if (username.isBlank() || username in ignoredAccounts) return 0L
        return prefs.getLong("trial_expiry_$username", 0L)
    }

    fun addTrialDays(username: String, days: Int = 7) {
        if (username.isBlank() || username in ignoredAccounts) return
        val currentExpiry = getTrialExpiryTimestamp(username)
        val now = System.currentTimeMillis()
        val base = if (currentExpiry > now) currentExpiry else now
        val newExpiry = base + days * 86400_000L
        prefs.edit().putLong("trial_expiry_$username", newExpiry).apply()
    }

    fun resetTrial(username: String) {
        val user = username.trim()
        if (user.isNotBlank()) {
            prefs.edit()
                .remove("trial_claimed_$user")
                .remove("trial_expiry_$user")
                .apply()
        }
    }

    fun getLastClearedSupportTime(username: String): Long {
        val user = username.trim().lowercase()
        if (user.isBlank()) return 0L
        return prefs.getLong("last_cleared_support_$user", 0L)
    }

    fun setLastClearedSupportTime(username: String, timestampSec: Long = System.currentTimeMillis() / 1000) {
        val user = username.trim().lowercase()
        if (user.isNotBlank()) {
            prefs.edit().putLong("last_cleared_support_$user", timestampSec).apply()
        }
    }

    fun clearAuth() {
        prefs.edit()
            .remove("auth_token")
            .remove("user_email")
            .remove("payment_email")
            .remove("current_username")
            .remove("accounts_list")
            .apply()
    }
}
