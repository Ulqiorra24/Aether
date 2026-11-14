package com.example.v2rayng.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(
    val token: String,
    val username: String,
    val subscriptionUrl: String? = null,
    val subscriptionContent: String? = null,
    val message: String = ""
)

class AuthRepository {

    suspend fun login(baseUrl: String, email: String, password: String): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/login",
                jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                }
            )
        }

    suspend fun register(baseUrl: String, email: String, password: String, paymentEmail: String = ""): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/register",
                jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                    if (paymentEmail.isNotBlank()) put("payment_email", paymentEmail.trim())
                }
            )
        }

    suspend fun updateProfile(baseUrl: String, email: String, paymentEmail: String, newPassword: String? = null): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/update",
                jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("payment_email", paymentEmail.trim())
                    if (!newPassword.isNullOrBlank()) put("new_password", newPassword)
                }
            )
        }

    suspend fun activate(baseUrl: String, email: String, subId: String, password: String): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/activate",
                jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("sub_id", subId.trim())
                    put("password", password)
                }
            )
        }

    private fun postJson(urlStr: String, jsonPayload: JSONObject): Result<AuthResult> {
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 12_000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { os ->
                os.write(jsonPayload.toString())
                os.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream ?: connection.inputStream, "UTF-8")).use { it.readText() }

            if (statusCode in 200..299) {
                val json = try { JSONObject(responseText) } catch (_: Exception) { JSONObject() }
                val token = json.optString("token", json.optString("access_token", "auth_token_ok"))
                val username = json.optString("username", json.optString("email", jsonPayload.optString("email")))
                val subUrl = if (json.has("subscription_url")) json.getString("subscription_url") else null
                val subContent = if (json.has("subscription_content")) json.getString("subscription_content") else null
                val msg = json.optString("message", "Успешная авторизация")

                Result.success(AuthResult(token, username, subUrl, subContent, msg))
            } else {
                val errorMsg = try {
                    JSONObject(responseText).optString("message", JSONObject(responseText).optString("error", "Ошибка сервера ($statusCode)"))
                } catch (_: Exception) {
                    "Ошибка сервера ($statusCode)"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
