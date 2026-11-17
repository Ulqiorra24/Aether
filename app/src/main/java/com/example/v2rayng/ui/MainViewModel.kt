package com.example.v2rayng.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.v2rayng.data.SubscriptionRepository
import com.example.v2rayng.data.VpnPrefs
import com.example.v2rayng.vpn.V2RayVpnService
import com.example.v2rayng.vpn.VpnPhase
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.v2rayng.vpn.V2RayConfigParser
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

data class VpnNode(
    val name: String,
    val uri: String,
    var latencyMs: Long = -1L
)

data class ServerLocationGroup(
    val locationName: String,
    val nodes: List<VpnNode>,
    val bestNodeIndex: Int,
    val bestNode: VpnNode,
    val bestLatencyMs: Long,
    val isSelected: Boolean
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable? = null,
    val isSystemApp: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = VpnPrefs(application)
    private val repository = SubscriptionRepository()
    private val authRepository = com.example.v2rayng.data.AuthRepository()

    val fixedHost = prefs.fixedHost

    var isLoggedIn by mutableStateOf(prefs.isLoggedIn)
        private set

    var userEmail by mutableStateOf(prefs.userEmail.ifBlank { prefs.currentUsername })
        private set

    var paymentEmail by mutableStateOf(prefs.paymentEmail)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    var isAuthLoading by mutableStateOf(false)
        private set

    var isPaymentLoading by mutableStateOf(false)
        private set

    var paymentError by mutableStateOf<String?>(null)

    var accounts by mutableStateOf(prefs.accounts)
        private set

    var username by mutableStateOf(prefs.currentUsername)
        private set

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    var subscriptionContent by mutableStateOf(prefs.getSubscription(username))
        private set

    var nodes by mutableStateOf<List<VpnNode>>(emptyList())
        private set

    private val remainingDaysPattern = Regex("""[⏳⌛]\uFE0F?\s*(\d+)\s*[dD](?![\p{L}\p{N}])""")

    // Subscription-provided snapshot, refreshed with the server list.
    val subscriptionDaysRemaining: Int?
        get() = nodes.firstNotNullOfOrNull { node ->
            remainingDaysPattern.find(node.name)?.groupValues?.get(1)?.toIntOrNull()
        }

    var selectedNodeIndex by mutableIntStateOf(0)
        private set

    var bypassLan by mutableStateOf(prefs.bypassLan)
        private set

    var bypassRu by mutableStateOf(prefs.bypassRu)
        private set

    var perAppMode by mutableStateOf(prefs.perAppMode)
        private set

    var perAppPackages by mutableStateOf(prefs.perAppPackages)
        private set

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var appSearchQuery by mutableStateOf("")

    var isAppsLoading by mutableStateOf(false)
        private set

    var showProtocols by mutableStateOf(prefs.showProtocols)
        private set

    var isAutoSelect by mutableStateOf(prefs.isAutoSelect)
        private set

    var logs by mutableStateOf<List<com.example.v2rayng.vpn.VpnLogEntry>>(emptyList())
        private set

    var vpnStatus by mutableStateOf(V2RayVpnService.status.value)
        private set
    val isConnected: Boolean get() = vpnStatus.phase == VpnPhase.CONNECTED
    val isConnecting: Boolean get() = vpnStatus.phase == VpnPhase.CONNECTING
    val isStopping: Boolean get() = vpnStatus.phase == VpnPhase.STOPPING

    var statusMessage by mutableStateOf("Готов к подключению")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var availableUpdate by mutableStateOf<com.example.v2rayng.data.UpdateInfo?>(null)
        private set
    var isDownloadingUpdate by mutableStateOf(false)
        private set

    var rxBytesPerSec by mutableStateOf(0L)
        private set

    var txBytesPerSec by mutableStateOf(0L)
        private set

    var downSpeedStr by mutableStateOf("0 Б/с")
        private set

    var upSpeedStr by mutableStateOf("0 Б/с")
        private set

    var connectionDurationStr by mutableStateOf("00:00:00")
        private set

    private var connectedAtTimestamp: Long = 0L

    private var speedTickerJob: Job? = null

    init {
        viewModelScope.launch {
            V2RayVpnService.status.collect {
                val wasConnected = isConnected
                vpnStatus = it
                statusMessage = it.message
                if (it.phase == VpnPhase.CONNECTED && !wasConnected) {
                    connectedAtTimestamp = System.currentTimeMillis()
                } else if (it.phase != VpnPhase.CONNECTED) {
                    connectedAtTimestamp = 0L
                    connectionDurationStr = "00:00:00"
                }
            }
        }
        viewModelScope.launch {
            V2RayVpnService.logs.collect {
                logs = it
            }
        }
        viewModelScope.launch {
            V2RayVpnService.currentConnectedUri.collect { uri ->
                if (uri != null) {
                    val idx = nodes.indexOfFirst { it.uri == uri }
                    if (idx != -1 && selectedNodeIndex != idx) {
                        selectedNodeIndex = idx
                    }
                }
            }
        }
        if (isLoggedIn && username.isNotBlank()) {
            val saved = decodeSubscriptionContent(prefs.getSubscription(username))
            if (saved.isNotBlank()) {
                subscriptionContent = saved
                parseNodes(saved)
            }
            fetchSubscriptionFor(username)
        } else {
            statusMessage = "Выполните вход для подключения"
        }
        checkForUpdates(application)
        startSpeedTicker()
    }

    private fun startSpeedTicker() {
        speedTickerJob?.cancel()
        speedTickerJob = viewModelScope.launch(Dispatchers.IO) {
            var lastRx = android.net.TrafficStats.getTotalRxBytes()
            var lastTx = android.net.TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()

            while (isActive) {
                delay(1000)
                if (!isConnected) {
                    connectedAtTimestamp = 0L
                    val currRx = android.net.TrafficStats.getTotalRxBytes()
                    val currTx = android.net.TrafficStats.getTotalTxBytes()
                    lastRx = currRx
                    lastTx = currTx
                    lastTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        rxBytesPerSec = 0L
                        txBytesPerSec = 0L
                        downSpeedStr = "0 Б/с"
                        upSpeedStr = "0 Б/с"
                        connectionDurationStr = "00:00:00"
                    }
                    continue
                }

                val now = System.currentTimeMillis()
                if (connectedAtTimestamp == 0L) {
                    connectedAtTimestamp = now
                }
                val elapsedMs = (now - connectedAtTimestamp).coerceAtLeast(0L)
                val totalSec = elapsedMs / 1000
                val hours = totalSec / 3600
                val minutes = (totalSec % 3600) / 60
                val seconds = totalSec % 60
                val formattedDuration = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                val currRx = android.net.TrafficStats.getTotalRxBytes()
                val currTx = android.net.TrafficStats.getTotalTxBytes()
                val currTime = System.currentTimeMillis()

                val timeDiff = (currTime - lastTime) / 1000.0
                if (timeDiff > 0 && lastRx >= 0 && lastTx >= 0 && currRx >= lastRx && currTx >= lastTx) {
                    val rxSpeed = ((currRx - lastRx) / timeDiff).toLong()
                    val txSpeed = ((currTx - lastTx) / timeDiff).toLong()
                    withContext(Dispatchers.Main) {
                        rxBytesPerSec = rxSpeed
                        txBytesPerSec = txSpeed
                        downSpeedStr = formatSpeed(rxSpeed)
                        upSpeedStr = formatSpeed(txSpeed)
                        connectionDurationStr = formattedDuration
                    }
                } else if (timeDiff > 0) {
                    val uidRx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
                    val uidTx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
                    if (uidRx >= 0 && uidTx >= 0 && lastRx >= 0 && lastTx >= 0 && uidRx >= lastRx && uidTx >= lastTx) {
                        val rxSpeed = ((uidRx - lastRx) / timeDiff).toLong()
                        val txSpeed = ((uidTx - lastTx) / timeDiff).toLong()
                        withContext(Dispatchers.Main) {
                            rxBytesPerSec = rxSpeed
                            txBytesPerSec = txSpeed
                            downSpeedStr = formatSpeed(rxSpeed)
                            upSpeedStr = formatSpeed(txSpeed)
                            connectionDurationStr = formattedDuration
                        }
                    }
                    lastRx = uidRx
                    lastTx = uidTx
                    lastTime = currTime
                    continue
                } else {
                    withContext(Dispatchers.Main) {
                        connectionDurationStr = formattedDuration
                    }
                }
                lastRx = currRx
                lastTx = currTx
                lastTime = currTime
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 Б/с"
        val k = 1024.0
        val sizes = arrayOf("Б/с", "КБ/с", "МБ/с", "ГБ/с")
        val i = (Math.log(bytesPerSec.toDouble()) / Math.log(k)).toInt().coerceIn(0, sizes.lastIndex)
        val value = bytesPerSec / Math.pow(k, i.toDouble())
        return if (i == 0) "${bytesPerSec} Б/с" else String.format(java.util.Locale.US, "%.1f %s", value, sizes[i])
    }

    fun checkForUpdates(context: android.content.Context) {
        if (com.example.v2rayng.BuildConfig.DEBUG) return
        viewModelScope.launch {
            try {
                val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                val update = com.example.v2rayng.data.UpdateChecker.checkForUpdate(version)
                if (update != null) {
                    availableUpdate = update
                }
            } catch (_: Exception) {}
        }
    }

    fun dismissUpdate() {
        availableUpdate = null
    }

    fun resetPaymentError() {
        paymentError = null
    }

    fun openPaymentUrl(
        context: android.content.Context,
        months: Int,
        promo: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val user = username.ifBlank { userEmail.ifBlank { accounts.firstOrNull() ?: "" } }
        if (user.isBlank()) {
            paymentError = "Сначала выполните вход"
            return
        }
        if (isPaymentLoading) return
        isPaymentLoading = true
        paymentError = null

        viewModelScope.launch {
            try {
                val encodedUser = java.net.URLEncoder.encode(user, "UTF-8")
                val promoQuery = if (promo.isNotBlank()) "&promo=${java.net.URLEncoder.encode(promo.trim(), "UTF-8")}" else ""
                val requestUrl = "$fixedHost/api/pay/create?email=$encodedUser&months=$months$promoQuery"
                val (paymentUrl, errMsg) = withContext(Dispatchers.IO) {
                    try {
                        val conn = (java.net.URL(requestUrl).openConnection() as java.net.HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 30000
                            readTimeout = 30000
                        }
                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().readText()
                            Pair(org.json.JSONObject(response).optString("payment_url"), null)
                        } else {
                            val errResponse = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                            val msg = try { org.json.JSONObject(errResponse ?: "").optString("message") } catch (_: Exception) { "" }
                            Pair(null, if (msg.isNotBlank()) msg else "Ошибка создания счета (${conn.responseCode})")
                        }
                    } catch (e: Exception) {
                        Pair(null, e.localizedMessage ?: "Ошибка подключения к серверу")
                    }
                }

                if (!paymentUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paymentUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onSuccess()
                        } catch (e: Exception) {
                            paymentError = "Не удалось открыть браузер: ${e.localizedMessage}"
                        }
                    }
                } else {
                    paymentError = errMsg ?: "Не удалось получить ссылку на оплату"
                }
            } catch (e: Exception) {
                paymentError = e.localizedMessage ?: "Не удалось открыть браузер для оплаты"
            } finally {
                isPaymentLoading = false
            }
        }
    }

    var updateDownloadedBytes by mutableStateOf(0L)
        private set
    var updateTotalBytes by mutableStateOf(0L)
        private set
    var updateDownloadError by mutableStateOf<String?>(null)
        private set

    fun downloadAndInstallUpdate(context: android.content.Context, updateInfo: com.example.v2rayng.data.UpdateInfo) {
        if (isDownloadingUpdate) return
        val appContext = context.applicationContext
        isDownloadingUpdate = true
        updateDownloadedBytes = 0L
        updateTotalBytes = 0L
        updateDownloadError = null
        viewModelScope.launch {
            try {
                val file = java.io.File(appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Aether-${updateInfo.tagName}.apk")
                val manager = appContext.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val id = withContext(Dispatchers.IO) {
                    if (file.exists()) check(file.delete()) { "Не удалось удалить предыдущий APK" }
                    manager.enqueue(android.app.DownloadManager.Request(android.net.Uri.parse(updateInfo.downloadUrl))
                        .setTitle("Aether ${updateInfo.tagName}")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationUri(android.net.Uri.fromFile(file))
                        .setAllowedOverMetered(true))
                }
                while (true) {
                    val progress = withContext(Dispatchers.IO) {
                        manager.query(android.app.DownloadManager.Query().setFilterById(id)).use { cursor ->
                            check(cursor.moveToFirst()) { "Загрузка не найдена" }
                            Triple(
                                cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS)),
                                cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                                cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            )
                        }
                    }
                    updateDownloadedBytes = progress.second.coerceAtLeast(0)
                    updateTotalBytes = progress.third.coerceAtLeast(0)
                    check(progress.first != android.app.DownloadManager.STATUS_FAILED) { "Не удалось скачать APK. Попробуйте снова." }
                    if (progress.first == android.app.DownloadManager.STATUS_SUCCESSFUL) break
                    delay(500)
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
                appContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                })
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                updateDownloadError = e.localizedMessage ?: "Ошибка обновления"
            } finally {
                isDownloadingUpdate = false
            }
        }
    }

    private var reconnectJob: Job? = null
    private var subscriptionJob: Job? = null
    private var pingJob: Job? = null

    fun selectAccount(newUsername: String, context: android.content.Context? = null) {
        subscriptionJob?.cancel()
        pingJob?.cancel()
        isLoading = false
        val wasActive = isConnected || isConnecting || isStopping
        username = newUsername
        prefs.currentUsername = newUsername
        if (newUsername.isBlank()) {
            subscriptionContent = ""
            nodes = emptyList()
            statusMessage = "Выполните вход для подключения"
            return
        }
        val saved = decodeSubscriptionContent(prefs.getSubscription(newUsername))
        if (saved.isNotBlank()) {
            subscriptionContent = saved
            parseNodes(saved)
        } else {
            subscriptionContent = ""
            nodes = emptyList()
        }
        if (wasActive && context != null) {
            if (nodes.isEmpty()) {
                val intent = Intent(context, V2RayVpnService::class.java).apply {
                    action = V2RayVpnService.ACTION_STOP
                }
                context.startService(intent)
                fetchSubscriptionFor(newUsername)
            } else {
                reconnectVpn(context)
            }
        } else {
            if (nodes.isEmpty()) {
                fetchSubscriptionFor(newUsername)
            } else {
                statusMessage = "Готов к подключению"
                pingNodes()
            }
        }
    }

    fun addAccount(newUsername: String, context: android.content.Context? = null) {
        val trimmed = newUsername.trim()
        if (trimmed.isNotBlank() && !accounts.contains(trimmed)) {
            val updated = accounts + trimmed
            accounts = updated
            prefs.accounts = updated
            selectAccount(trimmed, context)
        }
    }

    fun removeAccount(targetUsername: String, context: android.content.Context? = null) {
        if (targetUsername !in accounts) return
        val wasSelected = username == targetUsername
        accounts = accounts.filter { it != targetUsername }
        prefs.accounts = accounts
        prefs.removeSubscription(targetUsername)
        if (!wasSelected) return

        reconnectJob?.cancel()
        subscriptionJob?.cancel()
        pingJob?.cancel()
        isLoading = false
        val appContext = context ?: getApplication<Application>()
        if (accounts.isNotEmpty()) {
            selectAccount(accounts.first(), appContext)
        } else {
            if (isConnected || isConnecting || isStopping) {
                appContext.startService(Intent(appContext, V2RayVpnService::class.java).apply {
                    action = V2RayVpnService.ACTION_STOP
                })
            }
            username = ""
            prefs.currentUsername = ""
            subscriptionContent = ""
            nodes = emptyList()
            selectedNodeIndex = 0
            statusMessage = "Добавьте подписку для подключения"
        }
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.themeMode = mode
    }

    fun updateBypassLan(enabled: Boolean, context: android.content.Context? = null) {
        if (bypassLan == enabled) return
        bypassLan = enabled
        prefs.bypassLan = enabled
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun updateBypassRu(enabled: Boolean, context: android.content.Context? = null) {
        if (bypassRu == enabled) return
        bypassRu = enabled
        prefs.bypassRu = enabled
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun clearLogs() {
        V2RayVpnService.clearLogs()
    }

    fun loginUser(email: String, pass: String) {
        isAuthLoading = true
        authError = null
        viewModelScope.launch {
            val result = authRepository.login(fixedHost, email, pass)
            isAuthLoading = false
            result.onSuccess { authResult ->
                handleAuthSuccess(email, authResult)
            }.onFailure { error ->
                authError = error.localizedMessage ?: "Ошибка входа"
            }
        }
    }

    fun updatePaymentEmail(email: String) {
        paymentEmail = email
        prefs.paymentEmail = email
    }

    fun registerUser(email: String, pass: String, payEmail: String = "") {
        isAuthLoading = true
        authError = null
        if (payEmail.isNotBlank()) {
            updatePaymentEmail(payEmail)
        }
        viewModelScope.launch {
            val result = authRepository.register(fixedHost, email, pass, payEmail)
            isAuthLoading = false
            result.onSuccess { authResult ->
                handleAuthSuccess(email, authResult)
            }.onFailure { error ->
                authError = error.localizedMessage ?: "Ошибка регистрации"
            }
        }
    }

    fun updateProfile(newPayEmail: String, newPass: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val targetUser = username.ifBlank { userEmail }
            val result = authRepository.updateProfile(fixedHost, targetUser, newPayEmail, newPass)
            result.onSuccess {
                updatePaymentEmail(newPayEmail)
                onResult(true, "Профиль успешно обновлен")
            }.onFailure {
                updatePaymentEmail(newPayEmail)
                onResult(true, "Данные сохранены локально")
            }
        }
    }

    fun activateUser(email: String, subId: String, pass: String) {
        isAuthLoading = true
        authError = null
        viewModelScope.launch {
            val result = authRepository.activate(fixedHost, email, subId, pass)
            isAuthLoading = false
            result.onSuccess { authResult ->
                handleAuthSuccess(email, authResult)
            }.onFailure { error ->
                authError = error.localizedMessage ?: "Ошибка активации"
            }
        }
    }

    private fun decodeSubscriptionContent(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("vless://") || trimmed.startsWith("vmess://") || trimmed.startsWith("ss://") || trimmed.startsWith("trojan://")) {
            return trimmed
        }
        return try {
            val bytes = android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
            val decoded = String(bytes, Charsets.UTF_8).trim()
            if (decoded.contains("://")) decoded else trimmed
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun handleAuthSuccess(email: String, authResult: com.example.v2rayng.data.AuthResult) {
        prefs.authToken = authResult.token
        prefs.userEmail = email
        userEmail = email
        isLoggedIn = true
        authError = null
        if (!accounts.contains(email)) {
            val updated = accounts + email
            accounts = updated
            prefs.accounts = updated
        }
        username = email
        prefs.currentUsername = email

        val rawSub = authResult.subscriptionContent.orEmpty()
        val decoded = decodeSubscriptionContent(rawSub)
        if (decoded.contains("://")) {
            subscriptionContent = decoded
            prefs.saveSubscription(email, decoded)
            parseNodes(decoded)
        } else {
            fetchSubscriptionFor(email)
        }
    }

    fun logout(context: android.content.Context? = null) {
        reconnectJob?.cancel()
        subscriptionJob?.cancel()
        pingJob?.cancel()
        isLoading = false
        if (isConnected || isConnecting || isStopping) {
            val appContext = context ?: getApplication<Application>()
            appContext.startService(Intent(appContext, V2RayVpnService::class.java).apply {
                action = V2RayVpnService.ACTION_STOP
            })
        }
        prefs.clearAuth()
        prefs.currentUsername = ""
        prefs.accounts = emptyList()
        accounts = emptyList()
        isLoggedIn = false
        userEmail = ""
        username = ""
        paymentEmail = ""
        subscriptionContent = ""
        nodes = emptyList()
        selectedNodeIndex = 0
        authError = null
        paymentError = null
        statusMessage = "Выполните вход для подключения"
    }

    fun updateShowProtocols(enabled: Boolean) {
        showProtocols = enabled
        prefs.showProtocols = enabled
    }

    fun selectAutoFastest(context: android.content.Context? = null) {
        val bestIdx = getBestNodeIndex()
        if (bestIdx in nodes.indices) {
            val isDifferentNode = selectedNodeIndex != bestIdx
            selectedNodeIndex = bestIdx
            isAutoSelect = true
            prefs.isAutoSelect = true
            if ((isConnected || isConnecting || isStopping) && context != null && isDifferentNode) {
                reconnectVpn(context)
            }
        }
    }

    fun getBestNodeIndex(): Int {
        if (nodes.isEmpty()) return 0
        val measured = nodes.mapIndexed { idx, node -> Pair(idx, node) }
            .filter { it.second.latencyMs > 0 }
        if (measured.isNotEmpty()) {
            return measured.minByOrNull { it.second.latencyMs }?.first ?: 0
        }
        return 0
    }

    private val flagToCountryMap = mapOf(
        "🇳🇱" to "🇳🇱 Нидерланды",
        "🇫🇮" to "🇫🇮 Финляндия",
        "🇺🇸" to "🇺🇸 США",
        "🇩🇪" to "🇩🇪 Германия",
        "🇷🇴" to "🇷🇴 Румыния",
        "🇬🇧" to "🇬🇧 Великобритания",
        "🇫🇷" to "🇫🇷 Франция",
        "🇸🇬" to "🇸🇬 Сингапур",
        "🇹🇷" to "🇹🇷 Турция",
        "🇸🇪" to "🇸🇪 Швеция",
        "🇵🇱" to "🇵🇱 Польша",
        "🇨🇭" to "🇨🇭 Швейцария",
        "🇯🇵" to "🇯🇵 Япония",
        "🇰🇷" to "🇰🇷 Южная Корея",
        "🇨🇦" to "🇨🇦 Канада",
        "🇦🇺" to "🇦🇺 Австралия",
        "🇦🇹" to "🇦🇹 Австрия",
        "🇨🇿" to "🇨🇿 Чехия",
        "🇪🇸" to "🇪🇸 Испания",
        "🇮🇹" to "🇮🇹 Италия",
        "🇮🇪" to "🇮🇪 Ирландия",
        "🇧🇬" to "🇧🇬 Болгария"
    )

    fun cleanLocationName(rawName: String): String {
        var s = rawName.trim()
        s = s.replace(Regex("""(?i)\[(vless|vmess|trojan|ss|ssr|grpc|xhttp|hysteria|tuic|juicity|reality|tcp|ws|http)]"""), "")
        s = s.replace(Regex("""(?i)\((vless|vmess|trojan|ss|ssr|grpc|xhttp|hysteria|tuic|juicity|reality|tcp|ws|http)\)"""), "")
        s = s.replace(Regex("""(?i)\b(vless|vmess|trojan|grpc|xhttp|reality)\b"""), "")
        s = s.replace(Regex("""\s*[-#]\s*\d+\s*$"""), "").trim()

        flagToCountryMap[s]?.let { return it }

        return s.ifBlank { rawName.trim() }
    }

    fun getLocationGroups(): List<ServerLocationGroup> {
        if (nodes.isEmpty()) return emptyList()
        val map = LinkedHashMap<String, MutableList<Pair<Int, VpnNode>>>()
        nodes.forEachIndexed { index, node ->
            val location = cleanLocationName(node.name)
            map.getOrPut(location) { mutableListOf() }.add(Pair(index, node))
        }
        return map.map { (locName, nodePairs) ->
            val sortedByLatency = nodePairs.sortedBy { (_, node) ->
                if (node.latencyMs > 0) node.latencyMs else Long.MAX_VALUE
            }
            val (bestIndex, bestNode) = sortedByLatency.first()
            val isGroupSelected = !isAutoSelect && nodePairs.any { (idx, _) -> idx == selectedNodeIndex }
            ServerLocationGroup(
                locationName = locName,
                nodes = nodePairs.map { it.second },
                bestNodeIndex = bestIndex,
                bestNode = bestNode,
                bestLatencyMs = bestNode.latencyMs,
                isSelected = isGroupSelected
            )
        }
    }

    fun selectNode(index: Int, context: android.content.Context? = null) {
        if (index !in nodes.indices) return
        isAutoSelect = false
        prefs.isAutoSelect = false
        val isDifferentNode = selectedNodeIndex != index
        selectedNodeIndex = index
        if ((isConnected || isConnecting || isStopping) && context != null && isDifferentNode) {
            reconnectVpn(context)
        }
    }

    private fun reconnectVpn(context: android.content.Context) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            statusMessage = "Переключаем VPN…"
            val stopIntent = Intent(context, V2RayVpnService::class.java).apply {
                action = V2RayVpnService.ACTION_STOP
            }
            context.startService(stopIntent)
            withTimeoutOrNull(4000) {
                V2RayVpnService.status.first { it.phase == VpnPhase.DISCONNECTED || it.phase == VpnPhase.ERROR }
            }
            delay(300)
            startVpnServiceInternal(context)
        }
    }

    fun fetchSubscription() {
        fetchSubscriptionFor(username)
    }

    private fun fetchSubscriptionFor(targetUser: String) {
        if (targetUser.isBlank()) {
            statusMessage = "Введите имя пользователя"
            return
        }

        subscriptionJob?.cancel()
        isLoading = true
        statusMessage = "Загрузка подписки для $targetUser..."

        subscriptionJob = viewModelScope.launch {
            val result = repository.fetchSubscription(fixedHost, targetUser)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (targetUser != username || targetUser !in accounts) return@launch
            isLoading = false
            result.onSuccess { content ->
                val prevDays = subscriptionDaysRemaining
                val decoded = decodeSubscriptionContent(content)
                subscriptionContent = decoded
                prefs.saveSubscription(targetUser, decoded)
                parseNodes(decoded)
                checkSubscriptionExpiry(prevDays)
                statusMessage = "Готов к подключению (${nodes.size} узлов)"
                pingNodes()
            }.onFailure { error ->
                statusMessage = "Ошибка подписки: ${error.localizedMessage ?: "Сбой"}"
            }
        }
    }

    private fun checkSubscriptionExpiry(prevDays: Int?) {
        val days = subscriptionDaysRemaining
        if (days != null && days != prevDays) {
            if (days == 5 || days == 3 || days == 1) {
                val context = getApplication<android.app.Application>().applicationContext
                val prefs = context.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
                val lastNotifiedDays = prefs.getInt("last_notified_days_$username", -1)
                
                if (lastNotifiedDays != days) {
                    sendExpiryNotification(context, days)
                    prefs.edit().putInt("last_notified_days_$username", days).apply()
                }
            }
        }
    }

    private fun sendExpiryNotification(context: Context, daysLeft: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "subscription_alerts"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Оповещения о подписке", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания об окончании подписки Aether VPN"
            }
            manager.createNotificationChannel(channel)
        }
        
        val title = "Aether VPN: Подписка истекает"
        val message = when (daysLeft) {
            1 -> "Остался всего 1 день! Продлите подписку, чтобы не потерять доступ к безопасному интернету."
            else -> "Осталось $daysLeft дней. Не забудьте продлить подписку."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.v2rayng.R.mipmap.aether)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        manager.notify(2000 + daysLeft, notification)
    }

    private fun parseNodes(content: String) {
        val decoded = decodeSubscriptionContent(content)
        val validSchemes = setOf("vless", "vmess", "trojan", "ss", "ssr", "socks", "socks5", "hysteria", "hysteria2", "tuic", "juicity")
        nodes = decoded.lines()
            .filter { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || !trimmed.contains("://")) return@filter false
                val scheme = trimmed.substringBefore("://").lowercase()
                validSchemes.contains(scheme)
            }
            .mapNotNull { line ->
                val uri = line.trim()
                val parsed = try { V2RayConfigParser.parse(uri) } catch (_: Exception) { null }
                val rawName = if (uri.contains("#")) {
                    try {
                        java.net.URLDecoder.decode(uri.substringAfter("#"), "UTF-8")
                    } catch (_: Exception) {
                        null
                    }
                } else null
                val name = rawName?.ifBlank { null } ?: parsed?.name?.ifBlank { null } ?: extractFallbackName(uri, 0)
                VpnNode(name, uri)
            }
        if (nodes.isNotEmpty()) {
            if (selectedNodeIndex !in nodes.indices) {
                selectedNodeIndex = 0
            }
            if (!isConnected && !isConnecting && !isStopping) {
                statusMessage = "Готов к подключению"
            }
        } else {
            if (isLoggedIn) {
                statusMessage = "Загрузка подписки..."
            } else {
                statusMessage = "Выполните вход для подключения"
            }
        }
    }

    private fun extractFallbackName(uri: String, index: Int): String {
        return try {
            val mainPart = uri.substringBefore("#")
            val javaUri = URI(mainPart)
            val scheme = javaUri.scheme?.uppercase() ?: "PROXY"
            val host = javaUri.host ?: "Server ${index + 1}"
            "$scheme ($host)"
        } catch (e: Exception) {
            "Сервер ${index + 1}"
        }
    }

    fun pingNodes() {
        pingJob?.cancel()
        val sourceNodes = nodes
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            val updatedNodes = sourceNodes.map { node ->
                val latency = measureLatency(node.uri)
                node.copy(latencyMs = latency)
            }
            withContext(Dispatchers.Main) {
                nodes = updatedNodes
                if (isAutoSelect && nodes.isNotEmpty()) {
                    val bestIdx = getBestNodeIndex()
                    if (bestIdx in nodes.indices && bestIdx != selectedNodeIndex) {
                        selectedNodeIndex = bestIdx
                    }
                }
            }
        }
    }

    private fun isPrivateOrLocalHost(host: String): Boolean {
        val h = host.lowercase().trim()
        return h == "127.0.0.1" || h == "localhost" || h == "0.0.0.0" || h == "::1" ||
               h.startsWith("10.") || h.startsWith("192.168.") ||
               h.startsWith("172.16.") || h.startsWith("172.17.") || h.startsWith("172.18.") ||
               h.startsWith("172.19.") || h.startsWith("172.20.") || h.startsWith("172.21.") ||
               h.startsWith("172.22.") || h.startsWith("172.23.") || h.startsWith("172.24.") ||
               h.startsWith("172.25.") || h.startsWith("172.26.") || h.startsWith("172.27.") ||
               h.startsWith("172.28.") || h.startsWith("172.29.") || h.startsWith("172.30.") ||
               h.startsWith("172.31.")
    }

    private fun measureLatency(uriString: String): Long {
        val (host, port) = try {
            val parsed = V2RayConfigParser.parse(uriString)
            val targetHost = if (isPrivateOrLocalHost(parsed.host) && parsed.serverName.isNotBlank()) {
                parsed.serverName
            } else {
                parsed.host
            }
            Pair(targetHost, parsed.port)
        } catch (_: Exception) {
            try {
                val mainPart = uriString.substringBefore("#")
                val uri = URI(mainPart)
                val h = uri.host ?: return -1L
                val p = if (uri.port != -1) uri.port else 443
                Pair(h, p)
            } catch (_: Exception) {
                return -1L
            }
        }

        if (isPrivateOrLocalHost(host)) return -1L

        return try {
            val startTime = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
            }
            val elapsedNs = System.nanoTime() - startTime
            val elapsedMs = Math.round(elapsedNs / 1_000_000.0)
            if (elapsedMs <= 0) 1L else elapsedMs
        } catch (_: Exception) {
            -1L
        }
    }

    fun onVpnPermissionDenied() {
        statusMessage = "Разрешение VPN не предоставлено"
    }

    fun toggleVpn(context: android.content.Context) {
        if (isConnected || isConnecting) {
            reconnectJob?.cancel()
            val intent = Intent(context, V2RayVpnService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
            statusMessage = "VPN отключен"
        } else {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                statusMessage = "Требуется разрешение для VPN"
            } else {
                startVpnService(context)
            }
        }
    }

    fun startVpnService(context: android.content.Context) {
        if (isConnected || isConnecting || isStopping) return
        startVpnServiceInternal(context)
    }

    fun updatePerAppMode(mode: String, context: android.content.Context? = null) {
        if (perAppMode == mode) return
        perAppMode = mode
        prefs.perAppMode = mode
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun toggleAppPackage(pkgName: String, context: android.content.Context? = null) {
        val updated = if (perAppPackages.contains(pkgName)) {
            perAppPackages - pkgName
        } else {
            perAppPackages + pkgName
        }
        perAppPackages = updated
        prefs.perAppPackages = updated
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun selectAllApps(context: android.content.Context? = null) {
        val allPkgs = installedApps.map { it.packageName }.toSet()
        perAppPackages = allPkgs
        prefs.perAppPackages = allPkgs
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun clearSelectedApps(context: android.content.Context? = null) {
        perAppPackages = emptySet()
        prefs.perAppPackages = emptySet()
        if ((isConnected || isConnecting || isStopping) && context != null) {
            reconnectVpn(context)
        }
    }

    fun loadInstalledApps(context: android.content.Context) {
        if (installedApps.isNotEmpty() || isAppsLoading) return
        isAppsLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val ownPkg = context.packageName
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val apps = resolveInfos
                    .mapNotNull { ri ->
                        val pkg = ri.activityInfo.packageName
                        if (pkg == ownPkg) return@mapNotNull null
                        val appLabel = ri.loadLabel(pm).toString()
                        val icon = try { ri.loadIcon(pm) } catch (_: Exception) { null }
                        val appInfo = ri.activityInfo.applicationInfo
                        val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        AppInfo(pkg, appLabel, icon, isSystem)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }

                withContext(Dispatchers.Main) {
                    installedApps = apps
                    isAppsLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isAppsLoading = false
                }
            }
        }
    }

    private fun startVpnServiceInternal(context: android.content.Context) {
        val selectedUri = nodes.getOrNull(selectedNodeIndex)?.uri
        if (selectedUri == null) {
            statusMessage = "Выберите сервер из подписки"
            return
        }
        try {
            V2RayConfigParser.parse(selectedUri)
        } catch (e: IllegalArgumentException) {
            statusMessage = e.message ?: "Некорректная ссылка сервера"
            return
        }
        if (VpnService.prepare(context) != null) {
            statusMessage = "Требуется разрешение для VPN"
            return
        }

        val intent = Intent(context, V2RayVpnService::class.java).apply {
            putExtra("NODE_URI", selectedUri)
            putExtra("NODE_NAME", nodes.getOrNull(selectedNodeIndex)?.name)
            putStringArrayListExtra("ALL_NODE_URIS", ArrayList(nodes.map { it.uri }))
            putStringArrayListExtra("ALL_NODE_NAMES", ArrayList(nodes.map { it.name }))
            putExtra("BYPASS_LAN", bypassLan)
            putExtra("BYPASS_RU", bypassRu)
            putExtra("PER_APP_MODE", perAppMode)
            putStringArrayListExtra("PER_APP_PACKAGES", ArrayList(perAppPackages))
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            statusMessage = "Запуск VPN…"
        } catch (_: Exception) {
            statusMessage = "Не удалось запустить VPN-сервис"
        }
    }
}
