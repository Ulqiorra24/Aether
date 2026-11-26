package com.example.v2rayng.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.v2rayng.MainActivity
import com.example.v2rayng.data.VpnPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class AetherTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        statusJob?.cancel()
        statusJob = scope.launch {
            V2RayVpnService.status.collectLatest { status ->
                updateTileState(status.phase)
            }
        }
    }

    override fun onStopListening() {
        statusJob?.cancel()
        super.onStopListening()
    }

    private fun updateTileState(phase: VpnPhase) {
        val tile = qsTile ?: return
        val subText: String
        try {
            tile.icon = android.graphics.drawable.Icon.createWithResource(this, com.example.v2rayng.R.drawable.ic_qs_aether_logo)
        } catch (_: Exception) {}
        when (phase) {
            VpnPhase.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Aether VPN"
                subText = "Включён"
            }
            VpnPhase.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Aether VPN"
                subText = "Запуск…"
            }
            VpnPhase.STOPPING -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Aether VPN"
                subText = "Отключение…"
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Aether VPN"
                subText = "Отключён"
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subText
        }
        tile.updateTile()
    }

    private fun openAppAndCollapse(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                val method = TileService::class.java.getMethod("startActivityAndCollapse", Intent::class.java)
                method.invoke(this, intent)
            }
        } catch (_: Exception) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onClick() {
        super.onClick()
        val prefs = VpnPrefs(applicationContext)
        val currentStatus = V2RayVpnService.status.value.phase

        if (currentStatus == VpnPhase.CONNECTED || currentStatus == VpnPhase.CONNECTING) {
            val intent = Intent(this, V2RayVpnService::class.java).apply {
                action = V2RayVpnService.ACTION_STOP
            }
            startService(intent)
        } else {
            if (VpnService.prepare(applicationContext) != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                openAppAndCollapse(intent)
            } else {
                val username = prefs.currentUsername
                val subContent = prefs.getSubscription(username)
                if (subContent.isNotBlank()) {
                    val validSchemes = setOf("vless", "vmess", "trojan", "ss", "ssr", "socks", "socks5", "hysteria", "hysteria2", "tuic", "juicity")
                    val allUris = subContent.lines()
                        .map { it.trim() }
                        .filter { it.contains("://") && validSchemes.contains(it.substringBefore("://").lowercase()) }
                    val uri = allUris.firstOrNull()

                    if (uri != null) {
                        val nodeNames = allUris.map { u ->
                            try { V2RayConfigParser.parse(u).name } catch (_: Exception) { "Proxy" }
                        }
                        val intent = Intent(this, V2RayVpnService::class.java).apply {
                            putExtra("NODE_URI", uri)
                            putStringArrayListExtra("ALL_NODE_URIS", ArrayList(allUris))
                            putStringArrayListExtra("ALL_NODE_NAMES", ArrayList(nodeNames))
                            putExtra("BYPASS_LAN", prefs.bypassLan)
                            putExtra("BYPASS_RU", prefs.bypassRu)
                            putExtra("PER_APP_MODE", prefs.perAppMode)
                            putStringArrayListExtra("PER_APP_PACKAGES", ArrayList(prefs.perAppPackages))
                        }
                        androidx.core.content.ContextCompat.startForegroundService(this, intent)
                    } else {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        openAppAndCollapse(intent)
                    }
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    openAppAndCollapse(intent)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
