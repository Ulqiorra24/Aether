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
import com.example.v2rayng.vpn.V2RayConfigParser
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

    init {
        viewModelScope.launch {
            V2RayVpnService.status.collect {
                vpnStatus = it
                statusMessage = it.message
            }
        }
        viewModelScope.launch {
            V2RayVpnService.logs.collect {
                logs = it
            }
        }
        if (subscriptionContent.isNotBlank()) {
            parseNodes(subscriptionContent)
        } else if (username.isNotBlank()) {
            fetchSubscriptionFor(username)
        } else {
            statusMessage = "Добавьте подписку для подключения"
        }
        checkForUpdates(application)
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

    fun openPaymentUrl(context: android.content.Context, months: Int, promo: String = "") {
        val user = userEmail.ifBlank { username.ifBlank { accounts.firstOrNull() ?: "" } }
        if (user.isBlank()) {
            statusMessage = "Сначала выполните вход"
            return
        }
        viewModelScope.launch {
            try {
                val encodedUser = java.net.URLEncoder.encode(user, "UTF-8")
                val promoQuery = if (promo.isNotBlank()) "&promo=${java.net.URLEncoder.encode(promo.trim(), "UTF-8")}" else ""
                val requestUrl = "$fixedHost/api/pay/create?email=$encodedUser&months=$months$promoQuery"
                val paymentUrl = withContext(Dispatchers.IO) {
                    try {
                        val conn = (java.net.URL(requestUrl).openConnection() as java.net.HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                        }
                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().readText()
                            org.json.JSONObject(response).optString("payment_url")
                        } else {
                            val errResponse = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                            val msg = try { org.json.JSONObject(errResponse ?: "").optString("message") } catch (_: Exception) { "" }
                            withContext(Dispatchers.Main) {
                                statusMessage = if (msg.isNotBlank()) msg else "Ошибка создания счета"
                            }
                            null
                        }
                    } catch (_: Exception) { null }
                }

                if (!paymentUrl.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paymentUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                statusMessage = "Не удалось открыть браузер для оплаты"
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
            statusMessage = "Добавьте подписку для подключения"
            return
        }
        subscriptionContent = prefs.getSubscription(newUsername)
        parseNodes(subscriptionContent)
        if (wasActive && context != null) {
            if (subscriptionContent.isBlank()) {
                val intent = Intent(context, V2RayVpnService::class.java).apply {
                    action = V2RayVpnService.ACTION_STOP
                }
                context.startService(intent)
                fetchSubscriptionFor(newUsername)
            } else {
                reconnectVpn(context)
            }
        } else {
            if (subscriptionContent.isBlank()) {
                fetchSubscriptionFor(newUsername)
            } else {
                statusMessage = "Аккаунт переключен: $newUsername"
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
            val result = authRepository.updateProfile(fixedHost, userEmail, newPayEmail, newPass)
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

    private fun handleAuthSuccess(email: String, authResult: com.example.v2rayng.data.AuthResult) {
        prefs.authToken = authResult.token
        prefs.userEmail = email
        userEmail = email
        isLoggedIn = true
        authError = null
        addAccount(email)
        selectAccount(email)
        if (!authResult.subscriptionContent.isNullOrBlank()) {
            subscriptionContent = authResult.subscriptionContent
            prefs.saveSubscription(email, authResult.subscriptionContent)
            parseNodes(authResult.subscriptionContent)
        } else {
            fetchSubscriptionFor(email)
        }
    }

    fun logout(context: android.content.Context? = null) {
        if (isConnected || isConnecting || isStopping) {
            val appContext = context ?: getApplication<Application>()
            appContext.startService(Intent(appContext, V2RayVpnService::class.java).apply {
                action = V2RayVpnService.ACTION_STOP
            })
        }
        prefs.clearAuth()
        prefs.currentUsername = ""
        isLoggedIn = false
        userEmail = ""
        username = ""
        subscriptionContent = ""
        nodes = emptyList()
        selectedNodeIndex = 0
        statusMessage = "Выполните вход для подключения"
    }

    fun selectNode(index: Int, context: android.content.Context? = null) {
        if (index !in nodes.indices) return
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
                subscriptionContent = content
                prefs.saveSubscription(targetUser, content)
                parseNodes(content)
                statusMessage = "Подписка обновлена! Найдено узлов: ${nodes.size}"
                pingNodes()
            }.onFailure { error ->
                statusMessage = "Ошибка: ${error.localizedMessage ?: "Неизвестная ошибка"}"
            }
        }
    }

    private fun parseNodes(content: String) {
        val validSchemes = setOf("vless", "vmess", "trojan", "ss", "ssr", "socks", "socks5", "hysteria", "hysteria2", "tuic", "juicity")
        nodes = content.lines()
            .filter { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || !trimmed.contains("://")) return@filter false
                val scheme = trimmed.substringBefore("://").lowercase()
                validSchemes.contains(scheme)
            }
            .mapIndexed { index, line ->
                val uri = line.trim()
                val name = if (uri.contains("#")) {
                    try {
                        java.net.URLDecoder.decode(uri.substringAfter("#"), "UTF-8")
                    } catch (e: Exception) {
                        extractFallbackName(uri, index)
                    }
                } else {
                    extractFallbackName(uri, index)
                }
                VpnNode(name, uri)
            }
        if (selectedNodeIndex >= nodes.size) {
            selectedNodeIndex = 0
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
            }
        }
    }

    private fun measureLatency(uriString: String): Long {
        return try {
            val mainPart = uriString.substringBefore("#")
            val uri = URI(mainPart)
            val host = uri.host ?: return -1L
            val port = if (uri.port != -1) uri.port else 443
            val startTime = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2000)
            }
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
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
            putExtra("BYPASS_LAN", bypassLan)
            putExtra("BYPASS_RU", bypassRu)
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            statusMessage = "Запуск VPN…"
        } catch (_: Exception) {
            statusMessage = "Не удалось запустить VPN-сервис"
        }
    }
}
