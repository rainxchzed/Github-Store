package zed.rainxch.home.data.data_source.impl

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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.home.data.data_source.CachedRepositoriesDataSource
import zed.rainxch.home.data.dto.CachedRepoResponse
import zed.rainxch.home.domain.model.HomeCategory

class CachedRepositoriesDataSourceImpl(
    private val platform: Platform,
    private val logger: GitHubStoreLogger
) : CachedRepositoriesDataSource {
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

    override suspend fun getCachedTrendingRepos(): CachedRepoResponse? {
        return fetchCachedReposForCategory(HomeCategory.TRENDING)
    }

    override suspend fun getCachedHotReleaseRepos(): CachedRepoResponse? {
        return fetchCachedReposForCategory(HomeCategory.HOT_RELEASE)
    }

    override suspend fun getCachedMostPopularRepos(): CachedRepoResponse? {
        return fetchCachedReposForCategory(HomeCategory.MOST_POPULAR)
    }

    private suspend fun fetchCachedReposForCategory(
        category: HomeCategory
    ): CachedRepoResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val platformName = when (platform) {
                    Platform.ANDROID -> "android"
                    Platform.WINDOWS -> "windows"
                    Platform.MACOS -> "macos"
                    Platform.LINUX -> "linux"
                }

                val base = when (category) {
                    HomeCategory.TRENDING -> TRENDING_FULL_URL
                    HomeCategory.HOT_RELEASE -> HOT_RELEASE_FULL_URL
                    HomeCategory.MOST_POPULAR -> MOST_POPULAR_FULL_URL
                }

                val url = "$base/$platformName.json"

                logger.debug("🔍 Fetching cached repos from: $url")

                val response: HttpResponse = httpClient.get(url)

                logger.debug("📥 Response status: ${response.status.value} ${response.status.description}")

                when {
                    response.status.isSuccess() -> {
                        val responseText = response.bodyAsText()
                        logger.debug("📄 Response body length: ${responseText.length} characters")

                        val cachedData = json.decodeFromString<CachedRepoResponse>(responseText)

                        logger.debug("✓ Successfully loaded ${cachedData.repositories.size} cached repos")
                        logger.debug("✓ Last updated: ${cachedData.lastUpdated}")

                        cachedData
                    }

                    response.status.value == 404 -> {
                        logger.warn("⚠️ Cached data not found (404) - may not be generated yet")
                        logger.warn("⚠️ URL attempted: $url")
                        null
                    }

                    else -> {
                        val errorBody = response.bodyAsText()
                        logger.error("❌ Failed to fetch cached repos: HTTP ${response.status.value}")
                        logger.error("❌ Response body: ${errorBody.take(500)}")
                        null
                    }
                }
            } catch (e: HttpRequestTimeoutException) {
                logger.error("⏱️ Timeout fetching cached repos: ${e.message}")
                e.printStackTrace()
                null
            } catch (e: SerializationException) {
                logger.error("🔧 JSON parsing error: ${e.message}")
                e.printStackTrace()
                null
            } catch (e: Exception) {
                logger.error("💥 Error fetching cached repos: ${e.message}")
                logger.error("💥 Exception type: ${e::class.simpleName}")
                e.printStackTrace()
                null
            }
        }
    }

    private companion object {
        private const val BASE_REPO_URL = "https://raw.githubusercontent.com/OpenHub-Store/api/refs/heads"
        private const val TRENDING_FULL_URL = "$BASE_REPO_URL/main/cached-data/trending"
        private const val HOT_RELEASE_FULL_URL = "$BASE_REPO_URL/main/cached-data/new-releases"
        private const val MOST_POPULAR_FULL_URL = "$BASE_REPO_URL/main/cached-data/most-popular"


    }
}