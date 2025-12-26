package zed.rainxch.githubstore.feature.details.data.repository

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.GithubAsset
import zed.rainxch.githubstore.core.domain.model.GithubRelease
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.core.domain.model.GithubUser
import zed.rainxch.githubstore.core.domain.model.GithubUserProfile
import zed.rainxch.githubstore.feature.details.data.dto.GitLabUserNetworkModel
import zed.rainxch.githubstore.feature.details.data.dto.ReleaseNetwork
import zed.rainxch.githubstore.feature.details.data.dto.RepoByIdNetwork
import zed.rainxch.githubstore.feature.details.data.dto.RepoInfoNetwork
import zed.rainxch.githubstore.feature.details.data.dto.UserProfileNetwork
import zed.rainxch.githubstore.feature.details.data.mappers.toDomain
import zed.rainxch.githubstore.feature.details.data.utils.preprocessMarkdown
import zed.rainxch.githubstore.feature.details.domain.model.RepoStats
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.home.data.repository.dto.GitLabProjectNetworkModel
import zed.rainxch.githubstore.feature.home.data.repository.dto.GitLabReleaseNetworkModel
import zed.rainxch.githubstore.network.RateLimitException
import zed.rainxch.githubstore.network.safeApiCall

class DetailsRepositoryImpl(
    private val httpClient: HttpClient,
    private val appStateManager: AppStateManager,
    private val apiPlatform: ApiPlatform
) : DetailsRepository {

    override suspend fun getRepositoryById(id: Long): GithubRepoSummary {
        Logger.d { "Fetching repository by ID: $id on ${apiPlatform.name}" }
        return when (apiPlatform) {
            ApiPlatform.Github -> getGitHubRepositoryById(id)
            ApiPlatform.GitLab -> getGitLabRepositoryById(id)
        }
    }

    private suspend fun getGitHubRepositoryById(id: Long): GithubRepoSummary {
        val repoResult = httpClient.safeApiCall<RepoByIdNetwork>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.Github
        ) {
            get("repositories/$id") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }

        val repo = repoResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.Github)
            }
            throw error
        }

        return GithubRepoSummary(
            id = repo.id,
            name = repo.name,
            fullName = repo.fullName,
            owner = GithubUser(
                id = repo.owner.id,
                login = repo.owner.login,
                avatarUrl = repo.owner.avatarUrl,
                htmlUrl = repo.owner.htmlUrl
            ),
            description = repo.description,
            htmlUrl = repo.htmlUrl,
            stargazersCount = repo.stars,
            forksCount = repo.forks,
            language = repo.language,
            topics = repo.topics,
            releasesUrl = "https://api.github.com/repos/${repo.owner.login}/${repo.name}/releases{/id}",
            updatedAt = repo.updatedAt,
            defaultBranch = repo.defaultBranch
        )
    }

    private suspend fun getGitLabRepositoryById(id: Long): GithubRepoSummary {

        val repoResult = httpClient.safeApiCall<GitLabProjectNetworkModel>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.GitLab
        ) {
            get("projects/$id") {
                url.also { Logger.d { "Request URL: $it" } }
            }
        }

        val repo = repoResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.GitLab)
            }
            throw error
        }

        val encodedPath = repo.pathWithNamespace.replace("/", "%2F")

        return GithubRepoSummary(
            id = repo.id.toLong(),
            name = repo.name,
            fullName = repo.pathWithNamespace,
            owner = GithubUser(
                id = repo.namespace?.id?.toLong() ?: 0L,
                login = repo.namespace?.path ?: "",
                avatarUrl = repo.avatarUrl ?: (repo.namespace?.avatarUrl ?: ""),
                htmlUrl = repo.namespace?.let { "https://gitlab.com/${it.fullPath}" } ?: ""
            ),
            description = repo.description,
            htmlUrl = repo.webUrl,
            stargazersCount = repo.starCount,
            forksCount = repo.forksCount,
            language = null,
            topics = repo.topics,
            releasesUrl = "https://gitlab.com/api/v4/projects/$encodedPath/releases",
            updatedAt = repo.lastActivityAt,
            defaultBranch = repo.default_branch ?: "main"
        )
    }

    override suspend fun getLatestPublishedRelease(
        owner: String,
        repo: String,
        defaultBranch: String
    ): GithubRelease? {
        return when (apiPlatform) {
            ApiPlatform.Github -> getGitHubLatestRelease(owner, repo, defaultBranch)
            ApiPlatform.GitLab -> getGitLabLatestRelease(owner, repo, defaultBranch)
        }
    }

    private suspend fun getGitHubLatestRelease(
        owner: String,
        repo: String,
        defaultBranch: String
    ): GithubRelease? {
        val releasesResult = httpClient.safeApiCall<List<ReleaseNetwork>>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.Github
        ) {
            get("repos/$owner/$repo/releases") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                parameter("per_page", 10)
            }
        }

        releasesResult.onFailure { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.Github)
            }
        }

        val releases = releasesResult.getOrNull() ?: return null

        val latest = releases
            .asSequence()
            .filter { (it.draft != true) && (it.prerelease != true) }
            .sortedByDescending { it.publishedAt ?: it.createdAt ?: "" }
            .firstOrNull()
            ?: return null

        val processedLatestRelease = latest.copy(
            body = latest.body?.replace("<details>", "")
                ?.replace("</details>", "")
                ?.replace("<summary>", "")
                ?.replace("</summary>", "")
                ?.replace("\r\n", "\n")
                ?.let { rawMarkdown ->
                    preprocessMarkdown(
                        markdown = rawMarkdown,
                        baseUrl = "https://raw.githubusercontent.com/$owner/$repo/${defaultBranch}/"
                    )
                }
        )

        return processedLatestRelease.toDomain()
    }

    private suspend fun getGitLabLatestRelease(
        owner: String,
        repo: String,
        defaultBranch: String
    ): GithubRelease? {
        val projectPath = "$owner/$repo".replace("/", "%2F")

        val releasesResult = httpClient.safeApiCall<List<GitLabReleaseNetworkModel>>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.GitLab
        ) {
            get("projects/$projectPath/releases") {
                parameter("per_page", 10)
            }
        }

        releasesResult.onFailure { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.GitLab)
            }
        }

        val releases = releasesResult.getOrNull() ?: return null

        val latest = releases
            .firstOrNull { it.upcoming_release != true }
            ?: return null

        val processedDescription = latest.description
            ?.replace("\r\n", "\n")
            ?.let { rawMarkdown ->
                preprocessMarkdown(
                    markdown = rawMarkdown,
                    baseUrl = "https://gitlab.com/$owner/$repo/-/raw/$defaultBranch/"
                )
            }

        return GithubRelease(
            id = latest.tagName.hashCode().toLong(),
            tagName = latest.tagName,
            name = latest.name,
            author = GithubUser(
                id = 0L,
                login = owner,
                avatarUrl = "",
                htmlUrl = "https://gitlab.com/$owner"
            ),
            publishedAt = latest.releasedAt ?: latest.createdAt,
            description = processedDescription,
            assets = latest.assets?.links?.map { link ->
                GithubAsset(
                    id = link.id.toLong(),
                    name = link.name,
                    contentType = "application/octet-stream",
                    size = 0L,
                    downloadUrl = link.url,
                    uploader = GithubUser(
                        id = 0L,
                        login = owner,
                        avatarUrl = "",
                        htmlUrl = "https://gitlab.com/$owner"
                    )
                )
            } ?: emptyList(),
            tarballUrl = "https://gitlab.com/$owner/$repo/-/archive/${latest.tagName}/${repo}-${latest.tagName}.tar.gz",
            zipballUrl = "https://gitlab.com/$owner/$repo/-/archive/${latest.tagName}/${repo}-${latest.tagName}.zip",
            htmlUrl = "https://gitlab.com/$owner/$repo/-/releases/${latest.tagName}"
        )
    }

    override suspend fun getReadme(owner: String, repo: String, defaultBranch: String): String? {
        return when (apiPlatform) {
            ApiPlatform.Github -> getGitHubReadme(owner, repo, defaultBranch)
            ApiPlatform.GitLab -> getGitLabReadme(owner, repo, defaultBranch)
        }
    }

    private suspend fun getGitHubReadme(owner: String, repo: String, defaultBranch: String): String? {
        return try {
            val rawMarkdownResult = httpClient.safeApiCall<String>(
                rateLimitHandler = appStateManager.rateLimitHandler,
                autoRetryOnRateLimit = false,
                apiPlatform = ApiPlatform.Github
            ) {
                get("https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/README.md")
            }

            rawMarkdownResult.onFailure { error ->
                if (error is RateLimitException) {
                    appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.Github)
                }
            }

            val rawMarkdown = rawMarkdownResult.getOrNull()
                ?: throw Exception("Failed to fetch $defaultBranch README")

            val baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/"
            preprocessMarkdown(markdown = rawMarkdown, baseUrl = baseUrl)
        } catch (e: Throwable) {
            Logger.e { "Failed to fetch README: $e" }
            null
        }
    }

    private suspend fun getGitLabReadme(owner: String, repo: String, defaultBranch: String): String? {
        return try {
            val projectPath = "$owner/$repo".replace("/", "%2F")

            val rawMarkdownResult = httpClient.safeApiCall<String>(
                rateLimitHandler = appStateManager.rateLimitHandler,
                autoRetryOnRateLimit = false,
                apiPlatform = ApiPlatform.GitLab
            ) {
                get("projects/$projectPath/repository/files/README.md/raw") {
                    parameter("ref", defaultBranch)
                }
            }

            rawMarkdownResult.onFailure { error ->
                if (error is RateLimitException) {
                    appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.GitLab)
                }
            }

            val rawMarkdown = rawMarkdownResult.getOrNull()
                ?: throw Exception("Failed to fetch $defaultBranch README")

            val baseUrl = "https://gitlab.com/$owner/$repo/-/raw/$defaultBranch/"
            preprocessMarkdown(markdown = rawMarkdown, baseUrl = baseUrl)
        } catch (e: Throwable) {
            Logger.e { "Failed to fetch GitLab README: $e" }
            null
        }
    }

    override suspend fun getRepoStats(owner: String, repo: String): RepoStats {
        return when (apiPlatform) {
            ApiPlatform.Github -> getGitHubRepoStats(owner, repo)
            ApiPlatform.GitLab -> getGitLabRepoStats(owner, repo)
        }
    }

    private suspend fun getGitHubRepoStats(owner: String, repo: String): RepoStats {
        val infoResult = httpClient.safeApiCall<RepoInfoNetwork>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.Github
        ) {
            get("repos/$owner/$repo") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }

        val info = infoResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.Github)
            }
            throw error
        }

        return RepoStats(
            stars = info.stars,
            forks = info.forks,
            openIssues = info.openIssues,
        )
    }

    private suspend fun getGitLabRepoStats(owner: String, repo: String): RepoStats {
        val projectPath = "$owner/$repo".replace("/", "%2F")

        val infoResult = httpClient.safeApiCall<GitLabProjectNetworkModel>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.GitLab
        ) {
            get("projects/$projectPath")
        }

        val info = infoResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.GitLab)
            }
            throw error
        }

        return RepoStats(
            stars = info.starCount,
            forks = info.forksCount,
            openIssues = 0,
        )
    }

    override suspend fun getUserProfile(username: String): GithubUserProfile {
        return when (apiPlatform) {
            ApiPlatform.Github -> getGitHubUserProfile(username)
            ApiPlatform.GitLab -> getGitLabUserProfile(username)
        }
    }

    private suspend fun getGitHubUserProfile(username: String): GithubUserProfile {
        val userResult = httpClient.safeApiCall<UserProfileNetwork>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.Github
        ) {
            get("users/$username") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }

        val user = userResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.Github)
            }
            throw error
        }

        return GithubUserProfile(
            id = user.id,
            login = user.login,
            name = user.name,
            bio = user.bio,
            avatarUrl = user.avatarUrl,
            htmlUrl = user.htmlUrl,
            followers = user.followers,
            following = user.following,
            publicRepos = user.publicRepos,
            location = user.location,
            company = user.company,
            blog = user.blog,
            twitterUsername = user.twitterUsername
        )
    }

    private suspend fun getGitLabUserProfile(username: String): GithubUserProfile {
        val searchResult = httpClient.safeApiCall<List<GitLabUserNetworkModel>>(
            rateLimitHandler = appStateManager.rateLimitHandler,
            autoRetryOnRateLimit = false,
            apiPlatform = ApiPlatform.GitLab
        ) {
            get("users") {
                parameter("username", username)
            }
        }

        val users = searchResult.getOrElse { error ->
            if (error is RateLimitException) {
                appStateManager.updateRateLimit(error.rateLimitInfo, ApiPlatform.GitLab)
            }
            throw error
        }

        val user = users.firstOrNull() ?: throw Exception("User not found: $username")

        return GithubUserProfile(
            id = user.id.toLong(),
            login = user.username,
            name = user.name,
            bio = user.bio,
            avatarUrl = user.avatarUrl ?: "",
            htmlUrl = user.webUrl,
            followers = 0,
            following = 0,
            publicRepos = 0,
            location = user.location,
            company = user.organization,
            blog = user.websiteUrl,
            twitterUsername = user.twitter
        )
    }
}