package zed.rainxch.githubstore.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.core.data.TokenDataSource

/**
 * Build a Ktor client preconfigured for GitHub REST API.
 * It will append Authorization header when [getAccessToken] returns a non-empty token.
 */
fun buildGitHubHttpClient(getAccessToken: () -> String?): HttpClient {
    val json = Json { ignoreUnknownKeys = true }
    return HttpClient {
        install(ContentNegotiation) { json(json) }

        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { _, response ->
                val code = response.status.value
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
            val token = getAccessToken()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            header(HttpHeaders.UserAgent, "GithubStore/1.0 (KMP)")
        }
    }
}

fun buildAuthedGitHubHttpClient(tokenDataSource: TokenDataSource): HttpClient =
    buildGitHubHttpClient { tokenDataSource.current()?.accessToken }
