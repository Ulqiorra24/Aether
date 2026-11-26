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
    val contactEmail: String? = null,
    val subscriptionUrl: String? = null,
    val subscriptionContent: String? = null,
    val message: String = ""
)

data class RegisterResponse(
    val isVerificationRequired: Boolean = false,
    val challengeId: String? = null,
    val authResult: AuthResult? = null,
    val message: String = ""
)

class AuthRepository {

    suspend fun login(baseUrl: String, emailOrLogin: String, password: String): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/login",
                jsonPayload = JSONObject().apply {
                    put("email", emailOrLogin.trim())
                    put("login", emailOrLogin.trim())
                    put("username", emailOrLogin.trim())
                    put("password", password)
                }
            )
        }

    suspend fun registerStart(
        baseUrl: String,
        login: String,
        email: String,
        password: String,
        mode: String = "register",
        subId: String = ""
    ): Result<RegisterResponse> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("login", login.trim())
            put("email", email.trim())
            put("password", password)
            put("repeat_password", password)
            put("mode", mode)
            if (subId.isNotBlank()) put("sub_id", subId.trim())
        }

        val urlStr = "$baseUrl/api/account/register/start"
        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(payload.toString())
                os.flush()
            }

            val statusCode = conn.responseCode
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream ?: conn.inputStream, "UTF-8")).use { it.readText() }
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }

            val challengeId = json.optString("challenge_id", "")
            val status = json.optString("status", "")
            val msg = json.optString("message", "Код подтверждения отправлен на Email")

            if (status == "verification_required" || challengeId.isNotBlank()) {
                Result.success(RegisterResponse(isVerificationRequired = true, challengeId = challengeId, message = msg))
            } else if (statusCode in 200..299) {
                val token = json.optString("token", "auth_token_ok")
                val username = json.optString("username", login.ifBlank { email })
                val subUrl = if (json.has("subscription_url")) json.getString("subscription_url") else null
                val subContent = if (json.has("subscription_content")) json.getString("subscription_content") else null
                Result.success(RegisterResponse(isVerificationRequired = false, authResult = AuthResult(token, username, subUrl, subContent, msg), message = msg))
            } else {
                val errorMsg = json.optString("message", json.optString("error", "Ошибка регистрации ($statusCode)"))
                if (statusCode == 404) {
                    val legacyRes = if (mode == "activate") {
                        activate(baseUrl, email, subId, password)
                    } else {
                        register(baseUrl, email, password)
                    }
                    legacyRes.map { RegisterResponse(isVerificationRequired = false, authResult = it, message = it.message) }
                } else {
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            val legacyRes = if (mode == "activate") {
                activate(baseUrl, email, subId, password)
            } else {
                register(baseUrl, email, password)
            }
            legacyRes.map { RegisterResponse(isVerificationRequired = false, authResult = it, message = it.message) }
        }
    }

    suspend fun registerConfirm(
        baseUrl: String,
        challengeId: String,
        code: String
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        postJson(
            urlStr = "$baseUrl/api/account/register/confirm",
            jsonPayload = JSONObject().apply {
                put("challenge_id", challengeId.trim())
                put("code", code.trim())
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

    suspend fun claimTrial(baseUrl: String, usernameOrEmail: String): Result<AuthResult> =
        withContext(Dispatchers.IO) {
            postJson(
                urlStr = "$baseUrl/api/auth/trial",
                jsonPayload = JSONObject().apply {
                    put("email", usernameOrEmail.trim())
                    put("username", usernameOrEmail.trim())
                    put("trial_days", 7)
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
                val contactEmail = if (json.has("contact_email")) json.getString("contact_email")
                    else if (json.has("payment_email")) json.getString("payment_email")
                    else null
                val subUrl = if (json.has("subscription_url")) json.getString("subscription_url") else null
                val subContent = if (json.has("subscription_content")) json.getString("subscription_content") else null
                val msg = json.optString("message", "Успешная авторизация")

                Result.success(AuthResult(token, username, contactEmail, subUrl, subContent, msg))
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
