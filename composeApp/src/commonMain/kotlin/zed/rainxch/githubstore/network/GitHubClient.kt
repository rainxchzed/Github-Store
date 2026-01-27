package zed.rainxch.githubstore.network

import co.touchlab.kermit.Logger
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.rate_limit_exceeded
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource

fun buildGitHubHttpClient(
    getAccessToken: () -> String?,
    rateLimitHandler: RateLimitHandler? = null
): HttpClient {
    val json = Json { ignoreUnknownKeys = true }

    return HttpClient {
        install(ContentNegotiation) { json(json) }

        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }

        engine {
            // proxy = ProxyBuilder.http("")
        }

        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                val code = response.status.value

                rateLimitHandler?.updateFromHeaders(response.headers)

                if (code == 403) {
                    val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()
                    return@retryIf remaining == null || remaining > 0
                }

                code in 500..<600
            }

            retryOnExceptionIf { _, cause ->
                cause is HttpRequestTimeoutException ||
                        cause is UnresolvedAddressException ||
                        cause is IOException
            }

            exponentialDelay()
        }

        expectSuccess = false

        defaultRequest {
            url("https://api.github.com")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            header(HttpHeaders.UserAgent, "GithubStore/1.0 (KMP)")

            val token = getAccessToken()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}

fun buildAuthedGitHubHttpClient(
    tokenDataSource: TokenDataSource,
    rateLimitHandler: RateLimitHandler? = null
): HttpClient =
    buildGitHubHttpClient(
        getAccessToken = { tokenDataSource.current()?.accessToken },
        rateLimitHandler = rateLimitHandler
    )

suspend fun HttpResponse.checkRateLimit(rateLimitHandler: RateLimitHandler?) {
    rateLimitHandler?.updateFromHeaders(headers)

    if (status.value == 403) {
        val rateLimitInfo = RateLimitInfo.fromHeaders(headers)
        if (rateLimitInfo != null && rateLimitInfo.isExhausted) {
            throw RateLimitException(rateLimitInfo, getString(Res.string.rate_limit_exceeded))
        }
    }
}

suspend fun getErrorString(): String {
    return getString(Res.string.rate_limit_exceeded)
}

suspend inline fun <reified T> HttpClient.safeApiCall(
    rateLimitHandler: RateLimitHandler? = null,
    autoRetryOnRateLimit: Boolean = false,
    crossinline block: suspend HttpClient.() -> HttpResponse
): Result<T> {
    return try {
        if (rateLimitHandler != null && rateLimitHandler.isRateLimited()) {
            if (autoRetryOnRateLimit) {
                val waitTime = rateLimitHandler.getTimeUntilReset()
                Logger.d { "⏳ Rate limited, waiting ${waitTime}ms..." }
                kotlinx.coroutines.delay(waitTime + 1000)
            } else {
                return Result.failure(
                    exception = RateLimitException(
                        rateLimitInfo = rateLimitHandler.getCurrentRateLimit()!!,
                        getErrorString()
                    )
                )
            }
        }

        val response = block()

        response.checkRateLimit(rateLimitHandler)

        if (response.status.isSuccess()) {
            Result.success(response.body<T>())
        } else {
            Result.failure(
                Exception("HTTP ${response.status.value}: ${response.status.description}")
            )
        }
    } catch (e: RateLimitException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}