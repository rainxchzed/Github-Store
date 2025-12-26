package zed.rainxch.githubstore.feature.home.data.repository

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.core.data.mappers.toSummary
import zed.rainxch.githubstore.core.data.model.GithubRepoNetworkModel
import zed.rainxch.githubstore.core.data.model.GithubRepoSearchResponse
import zed.rainxch.githubstore.core.domain.Platform
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.feature.home.data.repository.dto.GitLabProjectNetworkModel
import zed.rainxch.githubstore.feature.home.data.repository.dto.GitLabReleaseNetworkModel
import zed.rainxch.githubstore.feature.home.data.repository.mappers.toGithubRepoNetworkModel
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import zed.rainxch.githubstore.feature.home.domain.model.PaginatedRepos
import zed.rainxch.githubstore.feature.home.domain.model.TrendingPeriod
import zed.rainxch.githubstore.network.RateLimitException
import zed.rainxch.githubstore.network.safeApiCall
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

class HomeRepositoryImpl(
    private val httpClient: HttpClient,
    private val platform: Platform,
    private val appStateManager: AppStateManager,
    private val apiPlatform: ApiPlatform
) : HomeRepository {

    @OptIn(ExperimentalTime::class)
    override fun getTrendingRepositories(page: Int): Flow<PaginatedRepos> {
        val oneWeekAgo = Clock.System.now()
            .minus(7.days)
            .toLocalDateTime(TimeZone.UTC)
            .date

        return when (apiPlatform) {
            ApiPlatform.Github -> searchGitHubReposWithInstallersFlow(
                baseQuery = "stars:>500 archived:false pushed:>=$oneWeekAgo",
                sort = "stars",
                order = "desc",
                startPage = page
            )

            ApiPlatform.GitLab -> searchGitLabProjectsWithInstallersFlow(
                minStars = 100,
                sort = "star_count",
                order = "desc",
                startPage = page
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun getNew(page: Int): Flow<PaginatedRepos> {
        val thirtyDaysAgo = Clock.System.now()
            .minus(30.days)
            .toLocalDateTime(TimeZone.UTC)
            .date

        return when (apiPlatform) {
            ApiPlatform.Github -> searchGitHubReposWithInstallersFlow(
                baseQuery = "stars:>5 archived:false created:>=$thirtyDaysAgo",
                sort = "created",
                order = "desc",
                startPage = page
            )

            ApiPlatform.GitLab -> searchGitLabProjectsWithInstallersFlow(
                minStars = 5,
                sort = "created_at",
                order = "desc",
                startPage = page
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun getRecentlyUpdated(page: Int): Flow<PaginatedRepos> {
        val threeDaysAgo = Clock.System.now()
            .minus(3.days)
            .toLocalDateTime(TimeZone.UTC)
            .date

        return when (apiPlatform) {
            ApiPlatform.Github -> searchGitHubReposWithInstallersFlow(
                baseQuery = "stars:>50 archived:false pushed:>=$threeDaysAgo",
                sort = "updated",
                order = "desc",
                startPage = page
            )

            ApiPlatform.GitLab -> searchGitLabProjectsWithInstallersFlow(
                minStars = 50,
                sort = "last_activity_at",
                order = "desc",
                startPage = page
            )
        }
    }

    private fun searchGitHubReposWithInstallersFlow(
        baseQuery: String,
        sort: String,
        order: String,
        startPage: Int,
        desiredCount: Int = 10
    ): Flow<PaginatedRepos> = flow {
        val results = mutableListOf<GithubRepoSummary>()
        var currentApiPage = startPage
        val perPage = 100
        val semaphore = Semaphore(25)
        val maxPagesToFetch = 5
        var pagesFetchedCount = 0
        var lastEmittedCount = 0

        val query = buildGitHubQuery(baseQuery)
        Logger.d { "GitHub Query: $query | Sort: $sort | Page: $startPage" }

        while (results.size < desiredCount && pagesFetchedCount < maxPagesToFetch) {
            currentCoroutineContext().ensureActive()

            try {
                val response = httpClient.safeApiCall<GithubRepoSearchResponse>(
                    apiPlatform = ApiPlatform.Github,
                    rateLimitHandler = appStateManager.rateLimitHandler,
                    autoRetryOnRateLimit = false
                ) {
                    get("/search/repositories") {
                        parameter("q", query)
                        parameter("sort", sort)
                        parameter("order", order)
                        parameter("per_page", perPage)
                        parameter("page", currentApiPage)
                    }
                }.getOrElse { error ->
                    handleSearchError(error, ApiPlatform.Github)
                    throw error
                }

                Logger.d { "GitHub Page $currentApiPage: Got ${response.items.size} repos" }

                if (response.items.isEmpty()) break

                processAndEmitResults(
                    candidates = response.items,
                    results = results,
                    lastEmittedCount = lastEmittedCount,
                    currentApiPage = currentApiPage,
                    desiredCount = desiredCount,
                    semaphore = semaphore,
                    onEmit = { batch, hasMore, nextPage ->
                        emit(PaginatedRepos(batch, hasMore, nextPage))
                        lastEmittedCount = results.size
                    }
                )

                if (results.size >= desiredCount || response.items.size < perPage) break

                currentApiPage++
                pagesFetchedCount++

            } catch (e: RateLimitException) {
                Logger.e { "Rate limited on GitHub" }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e { "GitHub search failed: ${e.message}" }
                break
            }
        }

        emitFinalResults(
            results,
            lastEmittedCount,
            currentApiPage,
            pagesFetchedCount,
            maxPagesToFetch,
            desiredCount
        )
    }.flowOn(Dispatchers.IO)

    private fun searchGitLabProjectsWithInstallersFlow(
        minStars: Int,
        sort: String,
        order: String,
        startPage: Int,
        desiredCount: Int = 10
    ): Flow<PaginatedRepos> = flow {
        if (!appStateManager.appState.value.isAuthenticated(apiPlatform)) {
            Logger.e { "Not authenticated for GitLab search - triggering login prompt" }
            appStateManager.triggerAuthDialog(apiPlatform)
            emit(PaginatedRepos(emptyList(), false, startPage))
            return@flow
        }

        val results = mutableListOf<GithubRepoSummary>()
        var currentApiPage = startPage
        val perPage = 100
        val semaphore = Semaphore(25)
        val maxPagesToFetch = 5
        var pagesFetchedCount = 0
        var lastEmittedCount = 0

        val searchTerm = getPlatformSearchTerm()
        Logger.d { "GitLab Projects: search=$searchTerm | order_by: $sort | sort: $order | Page: $startPage" }

        while (results.size < desiredCount && pagesFetchedCount < maxPagesToFetch) {
            currentCoroutineContext().ensureActive()

            try {
                val response = httpClient.safeApiCall<List<GitLabProjectNetworkModel>>(
                    apiPlatform = ApiPlatform.GitLab,
                    rateLimitHandler = appStateManager.rateLimitHandler,
                    autoRetryOnRateLimit = false
                ) {
                    get("projects") {
                        parameter("search", searchTerm)
                        parameter("order_by", sort)
                        parameter("sort", order)
                        parameter("per_page", perPage)
                        parameter("page", currentApiPage)
                        parameter("visibility", "public")
                        parameter("archived", false)
                    }
                }.getOrElse { error ->
                    handleSearchError(error, ApiPlatform.GitLab)
                    throw error
                }

                Logger.d { "GitLab Page $currentApiPage: Got ${response.size} projects" }

                if (response.isEmpty()) break

                val filtered = response.filter { project ->
                    project.starCount >= minStars
                }

                val githubFormatRepos = filtered.map { it.toGithubRepoNetworkModel() }

                processAndEmitResults(
                    candidates = githubFormatRepos,
                    results = results,
                    lastEmittedCount = lastEmittedCount,
                    currentApiPage = currentApiPage,
                    desiredCount = desiredCount,
                    semaphore = semaphore,
                    onEmit = { batch, hasMore, nextPage ->
                        emit(PaginatedRepos(batch, hasMore, nextPage))
                        lastEmittedCount = results.size
                    }
                )

                if (results.size >= desiredCount || response.size < perPage) break

                if (sort == "star_count" && order == "desc"
                    && (response.lastOrNull()?.starCount ?: 0) < minStars
                ) {
                    break
                }

                currentApiPage++
                pagesFetchedCount++

            } catch (e: RateLimitException) {
                Logger.e { "Rate limited on GitLab" }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e { "GitLab search failed: ${e.message}" }
                break
            }
        }

        emitFinalResults(
            results,
            lastEmittedCount,
            currentApiPage,
            pagesFetchedCount,
            maxPagesToFetch,
            desiredCount
        )
    }.flowOn(Dispatchers.IO)


    private suspend fun processAndEmitResults(
        candidates: List<GithubRepoNetworkModel>,
        results: MutableList<GithubRepoSummary>,
        lastEmittedCount: Int,
        currentApiPage: Int,
        desiredCount: Int,
        semaphore: Semaphore,
        onEmit: suspend (List<GithubRepoSummary>, Boolean, Int) -> Unit
    ) {
        val scored = candidates
            .map { repo -> repo to calculatePlatformScore(repo) }
            .filter { it.second > 0 }
            .take(50)
            .map { it.first }

        Logger.d { "Checking ${scored.size} candidates for installers" }

        coroutineScope {
            val deferredResults = scored.map { repo ->
                async {
                    semaphore.withPermit {
                        withTimeoutOrNull(5000) {
                            checkRepoHasInstallers(repo)
                        }
                    }
                }
            }

            for (deferred in deferredResults) {
                currentCoroutineContext().ensureActive()

                val result = deferred.await()
                if (result != null) {
                    results.add(result)
                    Logger.d { "Found installer repo: ${result.fullName} (${results.size}/$desiredCount)" }

                    if (results.size % 3 == 0 || results.size >= desiredCount) {
                        val newItems = results.subList(lastEmittedCount, results.size)
                        if (newItems.isNotEmpty()) {
                            onEmit(newItems.toList(), true, currentApiPage + 1)
                            Logger.d { "Emitted ${newItems.size} repos (total: ${results.size})" }
                        }
                    }

                    if (results.size >= desiredCount) break
                }
            }
        }
    }

    private suspend fun FlowCollector<PaginatedRepos>.emitFinalResults(
        results: List<GithubRepoSummary>,
        lastEmittedCount: Int,
        currentApiPage: Int,
        pagesFetchedCount: Int,
        maxPagesToFetch: Int,
        desiredCount: Int
    ) {
        if (results.size > lastEmittedCount) {
            val finalBatch = results.subList(lastEmittedCount, results.size)
            val finalHasMore = pagesFetchedCount < maxPagesToFetch && results.size >= desiredCount
            emit(
                PaginatedRepos(
                    repos = finalBatch.toList(),
                    hasMore = finalHasMore,
                    nextPageIndex = if (finalHasMore) currentApiPage + 1 else currentApiPage
                )
            )
            Logger.d { "Final emit: ${finalBatch.size} repos (total: ${results.size})" }
        } else if (results.isEmpty()) {
            emit(PaginatedRepos(emptyList(), false, currentApiPage))
            Logger.d { "No results found" }
        }
    }

    private fun handleSearchError(error: Throwable, platform: ApiPlatform) {
        Logger.e { "Search request failed on ${platform.name}: ${error.message}" }
        if ((platform == ApiPlatform.GitLab) && ((error.message?.contains("401") == true)
                    || (error.message?.contains("Authentication required") == true)
                    || (error.message?.contains("Unauthorized") == true))
        ) {
            appStateManager.triggerAuthDialog(platform)
        }
        if (error is RateLimitException) {
            appStateManager.updateRateLimit(error.rateLimitInfo, platform)
        }
    }

    private fun buildGitHubQuery(baseQuery: String): String {
        val topic = getPlatformSearchTerm()
        return "$baseQuery topic:$topic"
    }

    private fun getPlatformSearchTerm(): String {
        return when (platform.type) {
            PlatformType.ANDROID -> "android"
            PlatformType.WINDOWS -> "desktop"
            PlatformType.MACOS -> "macos"
            PlatformType.LINUX -> "linux"
        }
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
                if (topics.any {
                        it in setOf(
                            "desktop",
                            "electron",
                            "app",
                            "gui",
                            "compose-desktop"
                        )
                    }) score += 10
                if (topics.contains("cross-platform") || topics.contains("multiplatform")) score += 8
                if (language in setOf("kotlin", "c++", "rust", "c#", "swift", "dart")) score += 5
                if (desc.contains("desktop") || desc.contains("application")) score += 3
            }
        }

        return score
    }

    private suspend fun checkRepoHasInstallers(repo: GithubRepoNetworkModel): GithubRepoSummary? {
        return try {
            when (apiPlatform) {
                ApiPlatform.Github -> checkGitHubReleases(repo)
                ApiPlatform.GitLab -> checkGitLabReleases(repo)
            }
        } catch (e: Exception) {
            Logger.e { "Failed to check installers for ${repo.fullName}: ${e.message}" }
            null
        }
    }

    private suspend fun checkGitHubReleases(repo: GithubRepoNetworkModel): GithubRepoSummary? {
        val releases = httpClient.safeApiCall<List<GithubReleaseNetworkModel>>(
            apiPlatform = ApiPlatform.Github,
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false
        ) {
            get("/repos/${repo.fullName}/releases") {
                parameter("per_page", 10)
            }
        }.getOrNull() ?: return null

        val stableRelease = releases.firstOrNull {
            it.draft != true && it.prerelease != true
        } ?: return null

        if (stableRelease.assets.isEmpty()) return null

        val hasRelevantAssets = stableRelease.assets.any { asset ->
            isRelevantAsset(asset.name)
        }

        return if (hasRelevantAssets) repo.toSummary() else null
    }

    private suspend fun checkGitLabReleases(repo: GithubRepoNetworkModel): GithubRepoSummary? {
        val projectId = repo.fullName.replace("/", "%2F")

        val releases = httpClient.safeApiCall<List<GitLabReleaseNetworkModel>>(
            apiPlatform = ApiPlatform.GitLab,
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false
        ) {
            get("projects/$projectId/releases") {
                parameter("per_page", 10)
            }
        }.getOrNull() ?: return null

        val stableRelease = releases.firstOrNull { it.upcoming_release != true } ?: return null
        val links = stableRelease.assets?.links ?: return null
        if (links.isEmpty()) return null

        val hasRelevantAssets = links.any { link -> isRelevantAsset(link.name) }
        return if (hasRelevantAssets) repo.toSummary() else null
    }


    private fun isRelevantAsset(assetName: String): Boolean {
        val name = assetName.lowercase()
        return when (platform.type) {
            PlatformType.ANDROID -> name.endsWith(".apk")
            PlatformType.WINDOWS -> name.endsWith(".msi") || name.endsWith(".exe") || name.contains(
                ".exe"
            )

            PlatformType.MACOS -> name.endsWith(".dmg") || name.endsWith(".pkg")
            PlatformType.LINUX -> name.endsWith(".appimage") || name.endsWith(".deb") || name.endsWith(
                ".rpm"
            )
        }
    }

    @Serializable
    private data class GithubReleaseNetworkModel(
        val assets: List<AssetNetworkModel>,
        val draft: Boolean? = null,
        val prerelease: Boolean? = null,
        @SerialName("published_at") val publishedAt: String? = null
    )

    @Serializable
    private data class AssetNetworkModel(
        val name: String
    )
}