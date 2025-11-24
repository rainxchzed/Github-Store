package zed.rainxch.githubstore.feature.home.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import zed.rainxch.githubstore.core.domain.model.GithubUser
import zed.rainxch.githubstore.feature.home.data.model.GithubRepoNetworkModel
import zed.rainxch.githubstore.feature.home.data.model.GithubRepoSearchResponse
import zed.rainxch.githubstore.feature.home.data.model.toSummary
import zed.rainxch.githubstore.feature.home.domain.repository.PaginatedRepos
import kotlin.collections.filterNotNull

class HomeRepositoryImpl(
    private val githubNetworkClient: HttpClient,
    private val platform: Platform
) : HomeRepository {

    override fun getTrendingRepositories(page: Int): Flow<PaginatedRepos> =
        searchReposWithInstallersFlow("stars:>500", "stars", "desc", page)

    override fun getLatestUpdated(page: Int): Flow<PaginatedRepos> =
        searchReposWithInstallersFlow("stars:>50", "updated", "desc", page)

    override fun getNew(page: Int): Flow<PaginatedRepos> =
        searchReposWithInstallersFlow("stars:>10", "created", "desc", page)

    private fun searchReposWithInstallersFlow(
        baseQuery: String,
        sort: String,
        order: String,
        startPage: Int,
        desiredCount: Int = 30
    ): Flow<PaginatedRepos> = flow {
        val results = mutableListOf<GithubRepoSummary>()
        var page = startPage
        val perPage = 100
        val semaphore = Semaphore(25)
        val maxPagesToFetch = 3

        val query = buildSimplifiedQuery(baseQuery)
        println("GitHub Search Query: $query, starting page: $startPage")

        var pagesFetched = 0
        while (results.size < desiredCount && pagesFetched < maxPagesToFetch) {
            try {
                val response: GithubRepoSearchResponse =
                    githubNetworkClient.get("/search/repositories") {
                        parameter("q", query)
                        parameter("sort", sort)
                        parameter("order", order)
                        parameter("per_page", perPage)
                        parameter("page", page)
                    }.body()

                println("Page $page: Got ${response.items.size} repos from search")

                if (response.items.isEmpty()) break

                val candidates = response.items
                    .map { repo -> repo to calculatePlatformScore(repo) }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .take(50)
                    .map { it.first }

                println("Checking ${candidates.size} candidates for installers")

                coroutineScope {
                    val deferredResults = candidates.map { repo ->
                        async {
                            semaphore.withPermit {
                                withTimeoutOrNull(3000) {
                                    checkRepoHasInstallers(repo)
                                }
                            }
                        }
                    }

                    for (deferred in deferredResults) {
                        val result = deferred.await()
                        if (result != null) {
                            results.add(result)

                            // Emit intermediate results every 5 repos or when reaching desired count
                            if (results.size % 5 == 0 || results.size >= desiredCount) {
                                emit(PaginatedRepos(
                                    repos = results.toList(),
                                    hasMore = results.size < desiredCount
                                ))
                                println("Emitted: ${results.size} repos so far")
                            }

                            if (results.size >= desiredCount) {
                                break
                            }
                        }
                    }
                }

                if (results.size >= desiredCount || response.items.size < perPage) break
                page++
                pagesFetched++
            } catch (e: Exception) {
                println("Error in search: ${e.message}")
                e.printStackTrace()
                break
            }
        }

        // Final emit with complete results
        if (results.isNotEmpty()) {
            emit(PaginatedRepos(
                repos = results.take(desiredCount),
                hasMore = pagesFetched < maxPagesToFetch && results.size >= desiredCount
            ))
        }

        println("Finished: ${results.size} total repos")
    }.flowOn(Dispatchers.IO)

    private fun buildSimplifiedQuery(baseQuery: String): String {
        val topic = when (platform.type) {
            PlatformType.ANDROID -> "android"
            PlatformType.WINDOWS -> "desktop"
            PlatformType.MACOS -> "desktop"
            PlatformType.LINUX -> "linux"
        }

        return "$baseQuery topic:$topic"
    }

    private fun calculatePlatformScore(repo: GithubRepoNetworkModel): Int {
        var score = 5
        val topics = repo.topics.orEmpty().map { it.lowercase() }
        val language = repo.language?.lowercase()
        val desc = repo.description?.lowercase() ?: ""

        when (platform.type) {
            PlatformType.ANDROID -> {
                if (topics.contains("android")) score += 10
                if (topics.contains("mobile")) score += 5
                if (language == "kotlin" || language == "java") score += 5
                if (desc.contains("android") || desc.contains("apk")) score += 3
            }
            PlatformType.WINDOWS, PlatformType.MACOS, PlatformType.LINUX -> {
                if (topics.any { it in setOf("desktop", "electron", "app", "gui") }) score += 10
                if (topics.contains("cross-platform") || topics.contains("multiplatform")) score += 8
                if (language in setOf("kotlin", "c++", "rust", "c#", "swift")) score += 5
                if (desc.contains("desktop") || desc.contains("application")) score += 3
            }
        }

        return score
    }

    private suspend fun checkRepoHasInstallers(repo: GithubRepoNetworkModel): GithubRepoSummary? {
        return try {
            val release: GithubReleaseNetworkModel? = githubNetworkClient
                .get("/repos/${repo.owner.login}/${repo.name}/releases/latest") {
                    header("Accept", "application/vnd.github.v3+json")
                }
                .body()

            if (release == null || release.assets.isEmpty()) {
                return null
            }

            val relevantAssets = release.assets.filter { asset ->
                val name = asset.name.lowercase()
                when (platform.type) {
                    PlatformType.ANDROID -> name.endsWith(".apk")
                    PlatformType.WINDOWS -> name.endsWith(".msi") || name.endsWith(".exe") || name.contains(".exe")
                    PlatformType.MACOS -> name.endsWith(".dmg") || name.endsWith(".pkg")
                    PlatformType.LINUX -> name.endsWith(".appimage") || name.endsWith(".deb") || name.endsWith(".rpm")
                }
            }

            if (relevantAssets.isNotEmpty()) {
                println("${repo.fullName}: Found installers")
                repo.toSummary()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    private data class GithubReleaseNetworkModel(
        val assets: List<AssetNetworkModel>
    )

    @Serializable
    private data class AssetNetworkModel(
        val name: String
    )
}

// Platform interface to inject
interface Platform {
    val type: PlatformType
}

expect fun getPlatform(): Platform

enum class PlatformType {
    ANDROID,
    WINDOWS,
    MACOS,
    LINUX
}