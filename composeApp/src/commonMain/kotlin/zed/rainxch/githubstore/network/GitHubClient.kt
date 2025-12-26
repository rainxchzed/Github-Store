package zed.rainxch.githubstore.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                val code = response.status.value

                rateLimitHandler?.updateFromHeaders(response.headers, ApiPlatform.Github)

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

fun buildGitLabHttpClient(
    getAccessToken: () -> String?,
    rateLimitHandler: RateLimitHandler? = null,
    gitlabUrl: String = "https://gitlab.com"
): HttpClient {
    val json = Json { ignoreUnknownKeys = true }

    return HttpClient {
        install(ContentNegotiation) { json(json) }

        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }

        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                val code = response.status.value

                rateLimitHandler?.updateFromHeaders(response.headers, ApiPlatform.GitLab)

                if (code == 429) {
                    return@retryIf true
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
            url("$gitlabUrl/api/v4/")
            header(HttpHeaders.Accept, "application/json")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )

            val token = getAccessToken()?.trim().orEmpty()
            Logger.d { "GitLab token present: ${token.isNotEmpty()}, length: ${token.length}" }
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

fun buildAuthedGitLabHttpClient(
    tokenDataSource: TokenDataSource,
    rateLimitHandler: RateLimitHandler? = null,
    gitlabUrl: String = "https://gitlab.com"
): HttpClient {
    return buildGitLabHttpClient(
        getAccessToken = {
            runBlocking {
                val token = tokenDataSource.refreshIfNeeded(ApiPlatform.GitLab)
                token?.accessToken
            }
        },
        rateLimitHandler = rateLimitHandler,
        gitlabUrl = gitlabUrl
    )
}

fun HttpResponse.checkRateLimit(
    rateLimitHandler: RateLimitHandler?,
    apiPlatform: ApiPlatform
) {
    rateLimitHandler?.updateFromHeaders(headers, apiPlatform)

    val statusCode = status.value
    val isRateLimited = when (apiPlatform) {
        ApiPlatform.Github -> statusCode == 403
        ApiPlatform.GitLab -> statusCode == 429
    }

    if (isRateLimited) {
        val rateLimitInfo = RateLimitInfo.fromHeaders(headers, apiPlatform)
        if (rateLimitInfo != null && rateLimitInfo.isExhausted) {
            throw RateLimitException(rateLimitInfo)
        }
    }
}

suspend inline fun <reified T> HttpClient.safeApiCall(
    apiPlatform: ApiPlatform,
    rateLimitHandler: RateLimitHandler? = null,
    autoRetryOnRateLimit: Boolean = false,
    crossinline block: suspend HttpClient.() -> HttpResponse
): Result<T> {
    return try {
        if (rateLimitHandler != null && rateLimitHandler.isRateLimited(apiPlatform)) {
            if (autoRetryOnRateLimit) {
                val waitTime = rateLimitHandler.getTimeUntilReset(apiPlatform)
                Logger.d { "⏳ Rate limited on ${apiPlatform.name}, waiting ${waitTime}ms..." }
                kotlinx.coroutines.delay(waitTime + 1000)
            } else {
                return Result.failure(
                    exception = RateLimitException(
                        rateLimitInfo = rateLimitHandler.getCurrentRateLimit(apiPlatform)!!,
                        message = "Rate limit exceeded on ${apiPlatform.name}. Try again later or sign in for higher limits."
                    )
                )
            }
        }

        val response = block()

        response.checkRateLimit(rateLimitHandler, apiPlatform)

        if (response.status.isSuccess()) {
            Result.success(response.body<T>())
        } else if (response.status.value == 401 && apiPlatform == ApiPlatform.GitLab) {
            return Result.failure(Exception("Authentication required for ${apiPlatform.name}"))
        } else {
            return Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
        }
    } catch (e: RateLimitException) {
        Result.failure(e)
    } catch (e: Exception) {
        Logger.e { "Exception in safeApiCall: ${e::class.simpleName} - ${e.message}" }
        Result.failure(e)
    }
}