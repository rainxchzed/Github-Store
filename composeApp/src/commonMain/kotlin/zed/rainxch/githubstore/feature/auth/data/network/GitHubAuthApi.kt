package zed.rainxch.githubstore.feature.auth.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.domain.model.DeviceTokenError
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess

object GitHubAuthApi {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val http by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }

            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                retryOnException(maxRetries = 2, retryOnTimeout = true)
                exponentialDelay()
            }
        }
    }

    suspend fun startDeviceFlow(clientId: String, scope: String): DeviceStart {
        return withRetry(maxAttempts = 3, initialDelay = 1000) {
            val res = http.post("https://github.com/login/device/code") {
                accept(ContentType.Application.Json)
                headers.append(HttpHeaders.UserAgent, "GithubStore/1.0 (DeviceFlow)")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("scope", scope)
                        }
                    )
                )
            }
            val status = res.status
            val text = res.bodyAsText()

            if (status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
                error(
                    buildString {
                        append("GitHub device/code HTTP ")
                        append(status.value)
                        append(" ")
                        append(status.description)
                        append(". Body: ")
                        append(text.take(300))
                    }
                )
            }

            try {
                json.decodeFromString(DeviceStart.serializer(), text)
            } catch (_: Throwable) {
                try {
                    val err = json.decodeFromString(DeviceTokenError.serializer(), text)
                    error("${err.error}: ${err.errorDescription ?: ""}".trim())
                } catch (_: Throwable) {
                    error("Unexpected response from GitHub: $text")
                }
            }
        }
    }

    suspend fun pollDeviceToken(
        clientId: String,
        deviceCode: String
    ): Result<DeviceTokenSuccess> {
        return try {
            val res = http.post("https://github.com/login/oauth/access_token") {
                accept(ContentType.Application.Json)
                headers.append(HttpHeaders.UserAgent, "GithubStore/1.0 (DeviceFlow)")
                contentType(ContentType.Application.FormUrlEncoded)

                timeout {
                    socketTimeoutMillis = 30_000
                }

                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("device_code", deviceCode)
                            append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        }
                    )
                )
            }
            val status = res.status
            val text = res.body<String>()

            if (status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
                return Result.failure(
                    IllegalStateException(
                        "GitHub access_token HTTP ${status.value} ${status.description}"
                    )
                )
            }

            try {
                val ok = json.decodeFromString(DeviceTokenSuccess.serializer(), text)
                Result.success(ok)
            } catch (_: Throwable) {
                val err = json.decodeFromString(DeviceTokenError.serializer(), text)
                val message = buildString {
                    append(err.error)
                    val desc = err.errorDescription
                    if (!desc.isNullOrBlank()) {
                        append(": ")
                        append(desc)
                    }
                }
                Result.failure(IllegalStateException(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 5000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                println("⚠️ Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt == maxAttempts - 2) throw e
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }
}