package com.example.v2rayng.data

import kotlinx.coroutines.Dispatchers
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
                        return@withContext UpdateInfo(
                            tagName = tagName,
                            downloadUrl = downloadUrl,
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
