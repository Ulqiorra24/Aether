package com.example.v2rayng.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.v2rayng.MainActivity
import go.Seq
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
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

    companion object {
        const val ACTION_STOP = "STOP"
        private const val CHANNEL = "vpn"
        private const val NOTIFICATION_ID = 1
        private val lifecycleMutex = Mutex()
        private val mutableStatus = MutableStateFlow(VpnStatus())
        val status = mutableStatus.asStateFlow()
        val isConnected: Boolean get() = status.value.phase == VpnPhase.CONNECTED

        private val mutableLogs = MutableStateFlow<List<VpnLogEntry>>(emptyList())
        val logs = mutableLogs.asStateFlow()
        private var logCounter = 0L

        fun addLog(tag: String, message: String) {
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val entry = VpnLogEntry(++logCounter, timeStr, tag, message)
            mutableLogs.value = (listOf(entry) + mutableLogs.value).take(300)
        }

        fun clearLogs() {
            mutableLogs.value = emptyList()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent == null) {
            requestStop()
            return START_NOT_STICKY
        }
        if (starting || stopping || isConnected) return START_NOT_STICKY
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
        val bypassLan = intent.getBooleanExtra("BYPASS_LAN", true)
        val bypassRu = intent.getBooleanExtra("BYPASS_RU", true)

        addLog("INFO", "Запуск VPN (LAN bypass=$bypassLan, RU bypass=$bypassRu)")

        scope.launch {
            var errorMessage: String? = null
            withContext(Dispatchers.IO) {
                lifecycleMutex.withLock {
                    try {
                        check(prepare(this@V2RayVpnService) == null) { "Разрешение VPN отозвано" }
                        val node = V2RayConfigParser.parse(uri)
                        addLog("INFO", "Узел: ${node.name}")
                        val config = V2RayConfigParser.createConfig(node, bypassLan, bypassRu)
                        Seq.setContext(applicationContext)
                        Libv2ray.initCoreEnv(filesDir.absolutePath, "")
                        core = Libv2ray.newCoreController(object : CoreCallbackHandler {
                            override fun startup(): Long = 0
                            override fun shutdown(): Long = 0
                            override fun onEmitStatus(code: Long, message: String?): Long {
                                message?.let { raw ->
                                    if (raw.isNotBlank()) {
                                        val tag = when {
                                            raw.contains("udp:", ignoreCase = true) -> "UDP"
                                            raw.contains("tcp:", ignoreCase = true) -> "TCP"
                                            raw.contains("direct", ignoreCase = true) || raw.contains("freedom", ignoreCase = true) -> "DIRECT"
                                            raw.contains("proxy", ignoreCase = true) || raw.contains("vless", ignoreCase = true) -> "PROXY"
                                            raw.contains("[Warning]", ignoreCase = true) -> "WARN"
                                            raw.contains("[Error]", ignoreCase = true) -> "ERROR"
                                            else -> "INFO"
                                        }
                                        addLog(tag, raw.trim())
                                    }
                                }
                                return 0
                            }
                        })
                        if (stopRequested) return@withLock
                        tunInterface = Builder()
                            .setSession("Aether")
                            .setMtu(1500)
                            .addAddress("10.0.0.2", 30)
                            .addAddress("fd00:1:fd00:1::2", 126)
                            .addRoute("0.0.0.0", 0)
                            .addRoute("::", 0)
                            .addDnsServer("1.1.1.1")
                            .addDnsServer("8.8.8.8")
                            // Core sockets and subscription requests must use the underlying network.
                            .addDisallowedApplication(packageName)
                            .establish() ?: error("Не удалось создать VPN-интерфейс")
                        addLog("INFO", "Создан TUN интерфейс 10.0.0.2")
                        core!!.startLoop(config, tunInterface!!.fd)
                        check(core!!.isRunning) { "Ядро VPN не запустилось" }
                        addLog("INFO", "Ядро Xray запущен")
                        if (!stopRequested) verifyProxyConnection()
                    } catch (e: IllegalArgumentException) {
                        errorMessage = e.message ?: "Некорректная конфигурация сервера"
                        closeResources()
                    } catch (_: Exception) {
                        errorMessage = "Не удалось подключиться. Проверьте сервер и параметры REALITY в подписке."
                        closeResources()
                    } catch (_: LinkageError) {
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                addLog("INFO", "VPN успешно подключён!")
                mutableStatus.value = VpnStatus(VpnPhase.CONNECTED, "VPN подключён. Соединение через сервер проверено.")
                startForeground(NOTIFICATION_ID, notification("VPN подключён"))
            }
        }
        return START_NOT_STICKY
    }

    /** The probe explicitly uses Xray's SOCKS inbound because our own UID bypasses the VPN. */
    private fun verifyProxyConnection() {
        val socket = Socket(Proxy(Proxy.Type.SOCKS,
            InetSocketAddress("127.0.0.1", V2RayConfigParser.SOCKS_PORT)))
        probeSocket = socket
        try {
            socket.soTimeout = 10_000
            socket.connect(InetSocketAddress.createUnresolved("www.gstatic.com", 443), 10_000)
            ((SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(socket, "www.gstatic.com", 443, true) as SSLSocket).use { tls ->
                tls.soTimeout = 10_000
                tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
                tls.startHandshake()
                tls.outputStream.write(
                    "GET /generate_204 HTTP/1.1\r\nHost: www.gstatic.com\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII)
                )
                tls.outputStream.flush()
                val response = tls.inputStream.bufferedReader().readLine().orEmpty()
                check(response.startsWith("HTTP/1.1 204") || response.startsWith("HTTP/1.0 204"))
            }
        } finally {
            socket.close()
            probeSocket = null
        }
    }

    private fun requestStop(message: String = "VPN отключён") {
        if (stopping) return
        stopping = true
        stopRequested = true
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
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "VPN", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, V2RayVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Aether")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, "Отключить", stop)
            .build()
    }

    override fun onRevoke() {
        requestStop("Разрешение VPN отозвано")
    }

    override fun onDestroy() {
        stopRequested = true
        runCatching { probeSocket?.close() }
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
