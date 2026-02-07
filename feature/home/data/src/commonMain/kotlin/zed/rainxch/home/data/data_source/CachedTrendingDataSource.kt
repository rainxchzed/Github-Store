package zed.rainxch.home.data.data_source

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.core.domain.model.Platform

class CachedTrendingDataSource(
    private val platform: Platform
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }

        install(HttpRequestRetry) {
            maxRetries = 2
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }

        expectSuccess = false
    }

    private val baseUrl = "https://raw.githubusercontent.com/rainxchzed/Github-Store/main/cached-data/trending"

    /**
     * Fetch cached trending repositories for the current platform
     * Returns null if fetch fails or data is unavailable
     */
    suspend fun getCachedTrendingRepos(): CachedRepoResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val platformName = when (platform) {
                    Platform.ANDROID -> "android"
                    Platform.WINDOWS -> "windows"
                    Platform.MACOS -> "macos"
                    Platform.LINUX -> "linux"
                }

                val url = "$baseUrl/$platformName.json"

                Logger.d { "🔍 Fetching cached trending repos from: $url" }

                val response: HttpResponse = httpClient.get(url)

                Logger.d { "📥 Response status: ${response.status.value} ${response.status.description}" }

                when {
                    response.status.isSuccess() -> {
                        val responseText = response.bodyAsText()
                        Logger.d { "📄 Response body length: ${responseText.length} characters" }

                        val cachedData = json.decodeFromString<CachedRepoResponse>(responseText)

                        Logger.d { "✓ Successfully loaded ${cachedData.repositories.size} cached repos" }
                        Logger.d { "✓ Last updated: ${cachedData.lastUpdated}" }

                        cachedData
                    }
                    response.status.value == 404 -> {
                        Logger.w { "⚠️ Cached data not found (404) - may not be generated yet" }
                        Logger.w { "⚠️ URL attempted: $url" }
                        null
                    }
                    else -> {
                        val errorBody = response.bodyAsText()
                        Logger.e { "❌ Failed to fetch cached repos: HTTP ${response.status.value}" }
                        Logger.e { "❌ Response body: ${errorBody.take(500)}" }
                        null
                    }
                }
            } catch (e: HttpRequestTimeoutException) {
                Logger.e { "⏱️ Timeout fetching cached trending repos: ${e.message}" }
                e.printStackTrace()
                null
            } catch (e: SerializationException) {
                Logger.e { "🔧 JSON parsing error: ${e.message}" }
                e.printStackTrace()
                null
            } catch (e: Exception) {
                Logger.e { "💥 Error fetching cached trending repos: ${e.message}" }
                Logger.e { "💥 Exception type: ${e::class.simpleName}" }
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Clean up resources when done
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Cached repository data for a specific platform
 */
@Serializable
data class CachedRepoResponse(
    val platform: String,
    val lastUpdated: String,
    val totalCount: Int,
    val repositories: List<CachedGithubRepoSummary>
)

/**
 * Simplified repo summary for cached data
 * Only includes the fields present in the cached JSON files
 */
@Serializable
data class CachedGithubRepoSummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: CachedGithubOwner,
    val description: String?,
    val defaultBranch: String,
    val htmlUrl: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val topics: List<String>?,
    val releasesUrl: String,
    val updatedAt: String
)

/**
 * Simplified owner data for cached repos
 * Only includes login and avatarUrl (not id and htmlUrl)
 */
@Serializable
data class CachedGithubOwner(
    val login: String,
    val avatarUrl: String
)

/**
 * Extension to convert cached summary to full GithubRepoSummary
 */
fun CachedGithubRepoSummary.toGithubRepoSummary(): GithubRepoSummary {
    return GithubRepoSummary(
        id = id,
        name = name,
        fullName = fullName,
        owner = GithubUser(
            id = 0,
            login = owner.login,
            avatarUrl = owner.avatarUrl,
            htmlUrl = "https://github.com/${owner.login}"
        ),
        description = description,
        defaultBranch = defaultBranch,
        htmlUrl = htmlUrl,
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        language = language,
        topics = topics,
        releasesUrl = releasesUrl,
        updatedAt = updatedAt
    )
}