package com.example.v2rayng.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.v2rayng.MainActivity
import com.example.v2rayng.data.VpnPrefs
import go.Seq
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class VpnPhase { DISCONNECTED, CONNECTING, CONNECTED, STOPPING, ERROR }

data class VpnStatus(
    val phase: VpnPhase = VpnPhase.DISCONNECTED,
    val message: String = "Готов к подключению"
)

data class VpnLogEntry(
    val id: Long,
    val time: String,
    val tag: String, // "TCP", "UDP", "DIRECT", "PROXY", "INFO", "WARN", "ERROR"
    val message: String
)

class V2RayVpnService : VpnService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tunInterface: ParcelFileDescriptor? = null
    private var core: CoreController? = null
    private var starting = false
    private var stopping = false
    @Volatile private var stopRequested = false
    @Volatile private var probeSocket: Socket? = null
    private var watchdogJob: Job? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var isNetworkAvailable = true
    private val watchdogSignal = Channel<Unit>(Channel.CONFLATED)

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        try {
            connectivityManager = getSystemService(ConnectivityManager::class.java)
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isNetworkAvailable = true
                    try { setUnderlyingNetworks(arrayOf(network)) } catch (_: Exception) {}
                    addLog("INFO", "Сеть доступна, проверка VPN-соединения…")
                    watchdogSignal.trySend(Unit)
                }

                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                    addLog("WARN", "Сеть отключена")
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val isValid = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    isNetworkAvailable = isValid
                    if (isValid) {
                        try { setUnderlyingNetworks(arrayOf(network)) } catch (_: Exception) {}
                    }
                }
            }
            networkCallback = callback
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            addLog("WARN", "Не удалось зарегистрировать NetworkCallback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null
    }

    companion object {
        const val ACTION_STOP = "STOP"
        private const val CHANNEL = "vpn"
        private const val NOTIFICATION_ID = 1
        private val lifecycleMutex = Mutex()
        private val mutableStatus = MutableStateFlow(VpnStatus())
        val status = mutableStatus.asStateFlow()
        val isConnected: Boolean get() = status.value.phase == VpnPhase.CONNECTED

        val currentConnectedUri = MutableStateFlow<String?>(null)

        private val mutableLogs = MutableStateFlow<List<VpnLogEntry>>(emptyList())
        val logs = mutableLogs.asStateFlow()
        private var logCounter = 0L

        fun addLog(tag: String, message: String) {
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val entry = VpnLogEntry(++logCounter, timeStr, tag, message)
            mutableLogs.value = (listOf(entry) + mutableLogs.value).take(300)
        }

        fun parseCoreLogMessage(raw: String) {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return

            if (trimmed.contains(" accepted ", ignoreCase = true)) {
                val target = trimmed.substringAfter(" accepted ", "").trim()
                val outboundTag = when {
                    trimmed.contains("[direct]", ignoreCase = true) || trimmed.contains("-> direct", ignoreCase = true) -> "DIRECT"
                    trimmed.contains("[proxy]", ignoreCase = true) || trimmed.contains("-> proxy", ignoreCase = true) || trimmed.contains("-> vless", ignoreCase = true) -> "PROXY"
                    trimmed.contains("[block]", ignoreCase = true) || trimmed.contains("-> block", ignoreCase = true) -> "BLOCK"
                    else -> "TRAFFIC"
                }
                val dest = target.substringBefore(" [").substringBefore(" ->").trim()
                val formattedMsg = if (dest.isNotBlank()) "→ $outboundTag: $dest" else "→ $outboundTag: $target"
                addLog(outboundTag, formattedMsg)
                return
            }

            if (trimmed.contains("taking detour", ignoreCase = true)) {
                val detourTag = when {
                    trimmed.contains("[direct]", ignoreCase = true) -> "DIRECT"
                    trimmed.contains("[proxy]", ignoreCase = true) -> "PROXY"
                    trimmed.contains("[block]", ignoreCase = true) -> "BLOCK"
                    else -> "ROUTE"
                }
                val target = trimmed.substringAfter("for [", "").substringBefore("]", "").ifBlank {
                    trimmed.substringAfter("taking detour", "").trim()
                }
                val formattedMsg = if (target.isNotBlank()) "→ $detourTag: $target" else trimmed
                addLog(detourTag, formattedMsg)
                return
            }

            if (trimmed.contains("connection accepted", ignoreCase = true)) {
                val target = trimmed.substringAfter("connection accepted from ", "").trim()
                val formattedMsg = if (target.isNotBlank()) "Принято: $target" else trimmed
                addLog("INBOUND", formattedMsg)
                return
            }

            if (trimmed.contains("app/dns:", ignoreCase = true) || (trimmed.contains("domain ", ignoreCase = true) && trimmed.contains("->"))) {
                val dnsInfo = trimmed.substringAfter("app/dns:", "").trim().ifBlank { trimmed }
                addLog("DNS", "DNS: $dnsInfo")
                return
            }

            val tag = when {
                trimmed.contains("[direct]", ignoreCase = true) || trimmed.contains("freedom", ignoreCase = true) -> "DIRECT"
                trimmed.contains("[proxy]", ignoreCase = true) || trimmed.contains("vless", ignoreCase = true) -> "PROXY"
                trimmed.contains("[block]", ignoreCase = true) || trimmed.contains("blackhole", ignoreCase = true) -> "BLOCK"
                trimmed.contains("[Warning]", ignoreCase = true) || trimmed.contains("WARN", ignoreCase = true) -> "WARN"
                trimmed.contains("[Error]", ignoreCase = true) || trimmed.contains("ERROR", ignoreCase = true) -> "ERROR"
                trimmed.contains("udp:", ignoreCase = true) -> "UDP"
                trimmed.contains("tcp:", ignoreCase = true) -> "TCP"
                else -> "INFO"
            }

            val cleanMsg = trimmed.replace(Regex("""^\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}:\d{2}\s*"""), "")
                .replace(Regex("""^\[(Info|Warning|Error|Debug)\]\s*(\[\d+\]\s*)?"""), "").trim()
            addLog(tag, cleanMsg.ifBlank { trimmed })
        }

        fun clearLogs() {
            mutableLogs.value = emptyList()
        }

        fun extractAssets(context: android.content.Context) {
            val dir = context.filesDir
            for (name in listOf("geoip.dat", "geosite.dat")) {
                val target = File(dir, name)
                try {
                    val assetFd = try { context.assets.openFd(name) } catch (_: Exception) { null }
                    val assetLen = assetFd?.length ?: -1L
                    assetFd?.close()
                    val needsExtract = !target.exists() || target.length() == 0L || (assetLen > 0 && target.length() != assetLen)
                    if (needsExtract) {
                        val tmp = File(dir, "$name.tmp")
                        context.assets.open(name).use { input ->
                            tmp.outputStream().use { out -> input.copyTo(out) }
                        }
                        if (target.exists()) {
                            target.delete()
                        }
                        if (!tmp.renameTo(target)) {
                            tmp.copyTo(target, overwrite = true)
                            tmp.delete()
                        }
                        addLog("INFO", "База $name готова (${target.length() / 1024} КБ)")
                    }
                } catch (e: Exception) {
                    addLog("WARN", "Asset $name: ${e.message}")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent == null) {
            requestStop()
            return START_NOT_STICKY
        }
        if (starting || stopping || isConnected) return START_NOT_STICKY
        registerNetworkCallback()
        starting = true
        stopRequested = false
        mutableStatus.value = VpnStatus(VpnPhase.CONNECTING, "Подключение и проверка сервера…")
        try {
            startForeground(NOTIFICATION_ID, notification("Подключение…"))
        } catch (_: Exception) {
            starting = false
            mutableStatus.value = VpnStatus(VpnPhase.ERROR, "Не удалось запустить VPN-сервис")
            stopSelf()
            return START_NOT_STICKY
        }
        val uri = intent.getStringExtra("NODE_URI").orEmpty()
        val allNodeUris = intent.getStringArrayListExtra("ALL_NODE_URIS") ?: arrayListOf()
        val allNodeNames = intent.getStringArrayListExtra("ALL_NODE_NAMES") ?: arrayListOf()
        val bypassLan = intent.getBooleanExtra("BYPASS_LAN", true)
        val bypassRu = intent.getBooleanExtra("BYPASS_RU", true)
        val perAppMode = intent.getStringExtra("PER_APP_MODE")?.ifEmpty { null } ?: VpnPrefs(this).perAppMode
        val perAppPackages = intent.getStringArrayListExtra("PER_APP_PACKAGES")?.toSet() ?: VpnPrefs(this).perAppPackages

        addLog("INFO", "Запуск VPN (LAN bypass=$bypassLan, RU bypass=$bypassRu, PerApp mode=$perAppMode)")

        scope.launch {
            var errorMessage: String? = null
            withContext(Dispatchers.IO) {
                lifecycleMutex.withLock {
                    try {
                        check(prepare(this@V2RayVpnService) == null) { "Разрешение VPN отозвано" }
                        val node = V2RayConfigParser.parse(uri)
                        addLog("INFO", "Узел: ${node.name}")
                        extractAssets(applicationContext)
                        val hasGeoAssets = File(filesDir, "geosite.dat").exists() && File(filesDir, "geosite.dat").length() > 0 &&
                                           File(filesDir, "geoip.dat").exists() && File(filesDir, "geoip.dat").length() > 0
                        val config = V2RayConfigParser.createConfig(node, bypassLan, bypassRu, hasGeoAssets = hasGeoAssets, logDir = filesDir.absolutePath)
                        startLogTailing()
                        Seq.setContext(applicationContext)
                        Libv2ray.initCoreEnv(filesDir.absolutePath, "")
                        core = Libv2ray.newCoreController(object : CoreCallbackHandler {
                            override fun startup(): Long = 0
                            override fun shutdown(): Long = 0
                            override fun onEmitStatus(code: Long, message: String?): Long {
                                message?.let { parseCoreLogMessage(it) }
                                return 0
                            }
                        })
                        if (stopRequested) return@withLock
                        val prefs = VpnPrefs(this@V2RayVpnService)
                        val dnsServers = prefs.getDnsServers()
                        val killSwitch = prefs.killSwitchEnabled

                        val builder = Builder()
                            .setSession("Aether")
                            .setMtu(9000)
                            .addAddress("10.0.0.2", 30)
                            .addAddress("fd00:1:fd00:1::2", 126)
                            .addRoute("0.0.0.0", 0)
                            .addRoute("::", 0)
                            // Core sockets and subscription requests must use the underlying network.
                            .addDisallowedApplication(packageName)

                        dnsServers.forEach { dns ->
                            try { builder.addDnsServer(dns) } catch (_: Exception) {}
                        }

                        if (killSwitch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try { builder.setBlocking(true) } catch (_: Exception) {}
                        }

                        addLog("INFO", "DNS: ${prefs.dnsProvider} (${dnsServers.joinToString()}), KillSwitch: $killSwitch")

                        when (perAppMode) {
                            "ALLOW_SELECTED" -> {
                                perAppPackages.filter { it != packageName }.forEach { pkg ->
                                    try { builder.addAllowedApplication(pkg) } catch (_: Exception) {}
                                }
                            }
                            "DISALLOW_SELECTED" -> {
                                perAppPackages.filter { it != packageName }.forEach { pkg ->
                                    try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
                                }
                            }
                            else -> {
                                if (bypassRu) {
                                    val ruAppPackages = setOf(
                                        "com.bitrix24.android", "com.bitrix24.disk", "com.bitrix24.crm",
                                        "ru.sberbankmobile", "ru.sberbank.sberbusiness", "com.sberbank.sberpay",
                                        "ru.vtb24.mobilebanking.android", "ru.vtb.mobile.android", "com.vtbintegration.online",
                                        "ru.alfabank.mobile.android", "ru.alfastrah.app",
                                        "ru.tinkoff.tinkoffmobile", "com.tinkoff.mobile", "com.tbank.mobile",
                                        "ru.gazprombank.android.mobilebank.app", "ru.sovcombank.marusya", "ru.openbank.mobile",
                                        "ru.yandex.yandexbus", "ru.yandex.taxi", "ru.yandex.searchplugin", "ru.yandex.yandexnavi",
                                        "ru.yandex.disk", "ru.yandex.music", "ru.yandex.weatherplugin", "ru.yandex.market",
                                        "ru.gosuslugi.gost", "ru.gosuslugi.pos", "ru.nalog.NalogFL", "ru.rzd.pass",
                                        "ru.wildberries.work", "com.wildberries.ru", "ru.ozon.app.android",
                                        "ru.mts.mymts", "ru.tele2.mytele2", "ru.rt.life", "ru.megafon.ml", "ru.beeline.services"
                                    )
                                    ruAppPackages.forEach { pkg ->
                                        try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
                                    }
                                }
                            }
                        }

                        tunInterface = builder.establish() ?: error("Не удалось создать VPN-интерфейс")
                        addLog("INFO", "Создан TUN интерфейс 10.0.0.2")
                        core!!.startLoop(config, tunInterface!!.fd)
                        check(core!!.isRunning) { "Ядро VPN не запустилось" }
                        addLog("INFO", "Ядро Xray запущен")
                        if (!stopRequested) verifyProxyConnection()
                    } catch (e: IllegalArgumentException) {
                        errorMessage = e.message ?: "Некорректная конфигурация сервера"
                        addLog("ERROR", "Конфигурация: ${e.message}")
                        closeResources()
                    } catch (e: Exception) {
                        addLog("ERROR", "${e.javaClass.simpleName}: ${e.message}")
                        android.util.Log.e("V2RayVpnService", "Connection error", e)
                        errorMessage = when (e) {
                            is java.net.SocketTimeoutException -> "Время ожидания сервера истекло. Проверьте узел."
                            is java.net.ConnectException -> "Не удалось подключиться к серверу."
                            is IllegalStateException -> e.message ?: "Ядро VPN не запустилось"
                            else -> e.message?.takeIf { it.isNotBlank() }
                                ?: "Не удалось подключиться. Проверьте сервер и параметры REALITY в подписке."
                        }
                        closeResources()
                    } catch (e: LinkageError) {
                        addLog("ERROR", "Ошибка ядра: ${e.message}")
                        errorMessage = "Не удалось загрузить VPN-ядро для этого устройства"
                        closeResources()
                    }
                }
            }
            starting = false
            if (stopRequested) return@launch
            if (errorMessage != null) {
                addLog("ERROR", errorMessage!!)
                mutableStatus.value = VpnStatus(VpnPhase.ERROR, errorMessage!!)
                currentConnectedUri.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                addLog("INFO", "VPN успешно подключён!")
                mutableStatus.value = VpnStatus(VpnPhase.CONNECTED, "VPN подключён. Соединение через сервер проверено.")
                currentConnectedUri.value = uri
                startForeground(NOTIFICATION_ID, notification("VPN подключён"))
                startWatchdog(allNodeUris, allNodeNames, uri, bypassLan, bypassRu)
                startSupportPolling()
            }
        }
        return START_NOT_STICKY
    }

    private fun startWatchdog(
        allNodeUris: List<String>,
        allNodeNames: List<String>,
        initialUri: String,
        bypassLan: Boolean,
        bypassRu: Boolean
    ) {
        watchdogJob?.cancel()
        val candidateUris = if (allNodeUris.isNotEmpty()) allNodeUris else listOf(initialUri)
        if (candidateUris.size <= 1) return

        watchdogJob = scope.launch(Dispatchers.IO) {
            var currentUri = initialUri
            var failCount = 0
            while (isActive && !stopRequested) {
                withTimeoutOrNull(10_000L) {
                    watchdogSignal.receive()
                }
                if (stopRequested) break

                if (!isNetworkAvailable) {
                    failCount = 0
                    addLog("WARN", "Ожидание подключения к сети…")
                    continue
                }

                val isSuccess = probeProxyHealth()
                if (isSuccess) {
                    failCount = 0
                } else {
                    if (!isDeviceConnectedToInternet()) {
                        failCount = 0
                        addLog("WARN", "Устройство не подключено к интернету. Ожидание сети…")
                        continue
                    }

                    failCount++
                    addLog("WARN", "Проверка VPN не пройдена ($failCount/2)")
                    if (failCount >= 2) {
                        failCount = 0
                        addLog("WARN", "Потеря связи с сервером. Поиск рабочего узла…")

                        val bestWorkingUri = findBestWorkingNode(candidateUris, currentUri)
                        if (bestWorkingUri != null) {
                            currentUri = bestWorkingUri
                            val nextIndex = candidateUris.indexOf(currentUri)
                            val nextName = allNodeNames.getOrNull(nextIndex)
                                ?: try { V2RayConfigParser.parse(currentUri).name } catch (_: Exception) { "Backup" }

                            addLog("WARN", "Автовыбор переключает на сервер: $nextName")

                            withContext(Dispatchers.Main) {
                                try {
                                    android.widget.Toast.makeText(
                                        applicationContext,
                                        "Сервер автоматически сменён на $nextName из-за помех на линии",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (_: Exception) {}
                                mutableStatus.value = VpnStatus(VpnPhase.CONNECTED, "Автовыбор: переключение на $nextName…")
                            }

                            lifecycleMutex.withLock {
                                if (stopRequested) return@withLock
                                try {
                                    val node = V2RayConfigParser.parse(currentUri)
                                    val hasGeoAssets = File(filesDir, "geosite.dat").exists() && File(filesDir, "geosite.dat").length() > 0 &&
                                                       File(filesDir, "geoip.dat").exists() && File(filesDir, "geoip.dat").length() > 0
                                    val config = V2RayConfigParser.createConfig(node, bypassLan, bypassRu, hasGeoAssets = hasGeoAssets)

                                    core?.stopLoop()
                                    core?.startLoop(config, tunInterface!!.fd)
                                    addLog("INFO", "VPN автоматически переключён на: $nextName")
                                    currentConnectedUri.value = currentUri
                                    withContext(Dispatchers.Main) {
                                        mutableStatus.value = VpnStatus(VpnPhase.CONNECTED, "VPN подключён к $nextName")
                                        startForeground(NOTIFICATION_ID, notification("VPN подключён к $nextName"))
                                    }
                                } catch (e: Exception) {
                                    addLog("ERROR", "Ошибка смены сервера: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun probeProxyHealth(): Boolean {
        val targets = listOf(
            Pair("cp.cloudflare.com", 443),
            Pair("www.gstatic.com", 443),
            Pair("1.1.1.1", 53)
        )
        for ((host, port) in targets) {
            try {
                val socket = Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", V2RayConfigParser.SOCKS_PORT)))
                socket.use { s ->
                    s.soTimeout = 3_500
                    s.connect(InetSocketAddress.createUnresolved(host, port), 3_500)
                    if (port == 443) {
                        ((SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(s, host, port, true) as SSLSocket).use { tls ->
                            tls.soTimeout = 3_500
                            tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
                            tls.startHandshake()
                            tls.outputStream.write(
                                "GET /generate_204 HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII)
                            )
                            tls.outputStream.flush()
                            val response = tls.inputStream.bufferedReader().readLine().orEmpty()
                            if (response.contains("204") || response.contains("200")) {
                                return true
                            }
                        }
                    } else {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun isDeviceConnectedToInternet(): Boolean {
        if (!isNetworkAvailable) return false
        val cm = connectivityManager ?: getSystemService(ConnectivityManager::class.java)
        val activeNet = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53), 2_000)
                true
            }
        } catch (_: Exception) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("8.8.8.8", 53), 2_000)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private suspend fun findBestWorkingNode(
        allNodeUris: List<String>,
        currentUri: String
    ): String? = withContext(Dispatchers.IO) {
        if (allNodeUris.isEmpty()) return@withContext null

        val candidates = allNodeUris.map { uri ->
            async {
                val latency = measureNodeHostLatency(uri)
                Pair(uri, latency)
            }
        }.awaitAll()

        val workingCandidates = candidates.filter { it.second > 0 }.sortedBy { it.second }
        if (workingCandidates.isNotEmpty()) {
            val best = workingCandidates.firstOrNull { it.first != currentUri } ?: workingCandidates.first()
            best.first
        } else {
            val currIdx = allNodeUris.indexOf(currentUri)
            val nextIdx = if (currIdx != -1) (currIdx + 1) % allNodeUris.size else 0
            allNodeUris.getOrNull(nextIdx)
        }
    }

    private fun measureNodeHostLatency(uriString: String): Long {
        return try {
            val parsed = V2RayConfigParser.parse(uriString)
            val host = if (parsed.host == "127.0.0.1" || parsed.host == "localhost" || parsed.host.startsWith("10.")) {
                parsed.serverName.ifBlank { parsed.host }
            } else {
                parsed.host
            }
            val startTime = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, parsed.port), 2_000)
            }
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000L
            if (elapsedMs <= 0) 1L else elapsedMs
        } catch (_: Exception) {
            -1L
        }
    }

    /** The probe explicitly uses Xray's SOCKS inbound because our own UID bypasses the VPN. */
    private fun verifyProxyConnection() {
        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                Thread.sleep(250) // Small warm-up delay for Xray SOCKS inbound to bind
                val socket = Socket(Proxy(Proxy.Type.SOCKS,
                    InetSocketAddress("127.0.0.1", V2RayConfigParser.SOCKS_PORT)))
                probeSocket = socket
                try {
                    socket.soTimeout = 8_000
                    socket.connect(InetSocketAddress.createUnresolved("www.gstatic.com", 443), 8_000)
                    ((SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(socket, "www.gstatic.com", 443, true) as SSLSocket).use { tls ->
                        tls.soTimeout = 8_000
                        tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
                        tls.startHandshake()
                        tls.outputStream.write(
                            "GET /generate_204 HTTP/1.1\r\nHost: www.gstatic.com\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII)
                        )
                        tls.outputStream.flush()
                        val response = tls.inputStream.bufferedReader().readLine().orEmpty()
                        if (response.startsWith("HTTP/1.1 204") || response.startsWith("HTTP/1.0 204") ||
                            response.startsWith("HTTP/1.1 200") || response.startsWith("HTTP/1.0 200")) {
                            return
                        }
                    }
                } finally {
                    socket.close()
                    probeSocket = null
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        // Fallback: If HTTP 204 probe failed due to gstatic block, test simple TCP connection to 1.1.1.1:53 via SOCKS
        try {
            val socket = Socket(Proxy(Proxy.Type.SOCKS,
                InetSocketAddress("127.0.0.1", V2RayConfigParser.SOCKS_PORT)))
            probeSocket = socket
            try {
                socket.soTimeout = 5_000
                socket.connect(InetSocketAddress.createUnresolved("1.1.1.1", 53), 5_000)
                return
            } finally {
                socket.close()
                probeSocket = null
            }
        } catch (e: Exception) {
            throw lastException ?: e
        }
    }

    private var logTailJob: Job? = null

    private fun startLogTailing() {
        logTailJob?.cancel()
        logTailJob = scope.launch(Dispatchers.IO) {
            val accessFile = File(filesDir, "access.log")
            val errorFile = File(filesDir, "error.log")
            try {
                if (accessFile.exists()) accessFile.delete()
                accessFile.createNewFile()
                if (errorFile.exists()) errorFile.delete()
                errorFile.createNewFile()
            } catch (_: Exception) {}

            var accessOffset = 0L
            var errorOffset = 0L

            while (isActive) {
                try {
                    if (accessFile.exists() && accessFile.length() > accessOffset) {
                        java.io.RandomAccessFile(accessFile, "r").use { raf ->
                            raf.seek(accessOffset)
                            var line: String? = raf.readLine()
                            while (line != null) {
                                val utf8Line = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                                withContext(Dispatchers.Main) {
                                    parseCoreLogMessage(utf8Line)
                                }
                                line = raf.readLine()
                            }
                            accessOffset = raf.filePointer
                        }
                    }

                    if (errorFile.exists() && errorFile.length() > errorOffset) {
                        java.io.RandomAccessFile(errorFile, "r").use { raf ->
                            raf.seek(errorOffset)
                            var line: String? = raf.readLine()
                            while (line != null) {
                                val utf8Line = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                                withContext(Dispatchers.Main) {
                                    parseCoreLogMessage(utf8Line)
                                }
                                line = raf.readLine()
                            }
                            errorOffset = raf.filePointer
                        }
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    private fun requestStop(message: String = "VPN отключён") {
        if (stopping) return
        stopping = true
        stopRequested = true
        watchdogJob?.cancel()
        logTailJob?.cancel()
        currentConnectedUri.value = null
        addLog("INFO", "Остановка VPN...")
        mutableStatus.value = VpnStatus(VpnPhase.STOPPING, "Отключение…")
        runCatching { probeSocket?.close() }
        scope.launch {
            withContext(Dispatchers.IO) { lifecycleMutex.withLock { closeResources() } }
            addLog("INFO", message)
            mutableStatus.value = VpnStatus(VpnPhase.DISCONNECTED, message)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun closeResources() {
        runCatching { core?.stopLoop() }
        core = null
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    private fun notification(message: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "VPN Status", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Состояние VPN подключения Aether"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, V2RayVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.example.v2rayng.R.drawable.ic_qs_aether_logo)
            .setContentTitle("Aether VPN")
            .setContentText(message)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Отключить", stop)
            .build()
    }

    override fun onRevoke() {
        requestStop("Разрешение VPN отозвано")
    }

    private var supportPollingJob: Job? = null

    private fun startSupportPolling() {
        supportPollingJob?.cancel()
        supportPollingJob = scope.launch(Dispatchers.IO) {
            val prefs = VpnPrefs(applicationContext)
            val authRepo = com.example.v2rayng.data.AuthRepository()
            while (isActive) {
                try {
                    val host = prefs.fixedHost.removeSuffix("/")
                    val username = prefs.currentUsername
                    val email = prefs.userEmail
                    val currentNick = username.ifBlank { email.substringBefore("@") }
                    val currentMail = email.ifBlank { username }
                    if (currentNick.isNotBlank() || currentMail.isNotBlank()) {
                        val suppRes = authRepo.getSupportMessages(host, currentMail, currentNick)
                        if (suppRes.isSuccess) {
                            val messages = suppRes.getOrDefault(emptyList())
                            val clearedTime = prefs.getLastClearedSupportTime(currentNick)
                            val filtered = if (clearedTime > 0) messages.filter { it.createdAt > clearedTime } else messages
                            val lastNotified = prefs.lastNotifiedSupportMsgId
                            val newAdminMsgs = filtered.filter { it.isAdmin && it.id > lastNotified }.sortedBy { it.id }
                            if (newAdminMsgs.isNotEmpty()) {
                                for (msg in newAdminMsgs) {
                                    if (msg.id > prefs.lastNotifiedSupportMsgId) {
                                        prefs.lastNotifiedSupportMsgId = msg.id
                                        sendSupportPushNotification(applicationContext, msg.id, msg.message)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(15_000)
            }
        }
    }

    private fun sendSupportPushNotification(context: android.content.Context, msgId: Int, messageText: String) {
        val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "support_alerts"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Aether Поддержка", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления об ответах от службы поддержки Aether"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, com.example.v2rayng.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_tab", 3)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3000 + msgId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.v2rayng.R.drawable.ic_qs_aether_logo)
            .setContentTitle("Aether Поддержка")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        manager.notify(3000 + msgId, notification)
    }

    override fun onDestroy() {
        stopRequested = true
        supportPollingJob?.cancel()
        unregisterNetworkCallback()
        runCatching { probeSocket?.close() }
        currentConnectedUri.value = null
        if (mutableStatus.value.phase != VpnPhase.ERROR) {
            mutableStatus.value = VpnStatus(VpnPhase.DISCONNECTED, "VPN отключён")
        }
        // Cleanup is serialized across service instances and never blocks the main thread.
        scope.launch {
            withContext(Dispatchers.IO) { lifecycleMutex.withLock { closeResources() } }
            scope.cancel()
        }
        super.onDestroy()
    }
}
