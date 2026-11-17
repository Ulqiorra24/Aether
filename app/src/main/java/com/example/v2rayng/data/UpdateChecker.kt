package com.example.v2rayng.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/Ulqiorra24/Aether/releases/latest"

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val tagName = json.optString("tag_name", "")
                val body = json.optString("body", "")

                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (tagName.isNotBlank() && downloadUrl.isNotBlank()) {
                    val cleanRemote = tagName.removePrefix("v").trim()
                    val cleanCurrent = currentVersionName.removePrefix("v").trim()

                    if (isNewerVersion(cleanRemote, cleanCurrent)) {
                        // Официальный URL для скачивания на вашем сервере
                        val customUrl = "https://app.mikaela-go.ru/download/android?v=${System.currentTimeMillis()}"
                        
                        // Также добавим параметр к GitHub URL на всякий случай
                        val githubUrlBusted = if (downloadUrl.contains("?")) "$downloadUrl&v=${System.currentTimeMillis()}" else "$downloadUrl?v=${System.currentTimeMillis()}"
                        
                        // Определяем, откуда быстрее скачивать
                        val fastestUrl = getFastestUrl(githubUrlBusted, customUrl)

                        return@withContext UpdateInfo(
                            tagName = tagName,
                            downloadUrl = fastestUrl,
                            releaseNotes = body
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private suspend fun getFastestUrl(githubUrl: String, customUrl: String): String = coroutineScope {
        // Запускаем 2 параллельных HEAD-запроса, чтобы измерить отклик
        val gitDeferred = async(Dispatchers.IO) { measurePing(githubUrl) }
        val customDeferred = async(Dispatchers.IO) { measurePing(customUrl) }

        val gitPing = gitDeferred.await()
        val customPing = customDeferred.await()

        // Выбираем наш сервер, если он ответил успешно (код 200-399) 
        // и его отклик быстрее, либо если GitHub вообще не ответил
        if (customPing != -1L && (gitPing == -1L || customPing <= gitPing)) {
            return@coroutineScope customUrl
        }
        
        // Если дошли сюда, значит GitHub ответил быстрее, либо сайт лежит/ответил ошибкой.
        // Защита от возврата битой ссылки:
        if (gitPing != -1L) {
            return@coroutineScope githubUrl
        }
        
        // Фоллбэк, если всё же нужно вернуть customUrl 
        // (хотя customPing тоже == -1L, но пользователь всё равно увидит ошибку в DownloadManager)
        return@coroutineScope customUrl
    }

    private fun measurePing(urlString: String): Long {
        return try {
            val startTime = System.currentTimeMillis()
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.instanceFollowRedirects = true // GitHub редиректит на aws/githubusercontent
            connection.connect()
            
            if (connection.responseCode in 200..399) {
                System.currentTimeMillis() - startTime
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val rParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(rParts.size, cParts.size)) {
                val r = rParts.getOrNull(i) ?: 0
                val c = cParts.getOrNull(i) ?: 0
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
