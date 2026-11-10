package com.example.v2rayng.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class SubscriptionRepository {
    suspend fun fetchSubscription(host: String, username: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.trimEnd('/')
            val urlString = if (cleanHost.endsWith("/sub")) {
                "$cleanHost/$username"
            } else if (cleanHost.contains("/sub/")) {
                cleanHost
            } else {
                "$cleanHost/sub/$username"
            }

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                if (this is HttpsURLConnection) {
                    try {
                        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
                        })
                        val sc = SSLContext.getInstance("TLS")
                        sc.init(null, trustAllCerts, java.security.SecureRandom())
                        this.sslSocketFactory = sc.socketFactory
                        this.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
                    } catch (_: Exception) {}
                }
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Aether-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val decoded = try {
                    val bytes = Base64.decode(responseBody.trim(), Base64.DEFAULT)
                    String(bytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    responseBody
                }
                Result.success(decoded)
            } else {
                Result.failure(Exception("HTTP error $responseCode: Subscription not found for '$username'"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("${e.javaClass.simpleName}: ${e.localizedMessage ?: "Network error"}"))
        }
    }
}
