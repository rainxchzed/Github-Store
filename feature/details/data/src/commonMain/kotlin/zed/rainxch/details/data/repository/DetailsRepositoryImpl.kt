package zed.rainxch.details.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import zed.rainxch.core.data.network.executeRequest
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.data.dto.RepoByIdNetwork
import zed.rainxch.core.data.dto.RepoInfoNetwork
import zed.rainxch.core.data.dto.UserProfileNetwork
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.data.mappers.toDomain
import zed.rainxch.details.data.utils.ReadmeLocalizationHelper
import zed.rainxch.details.data.utils.preprocessMarkdown
import zed.rainxch.details.domain.model.RepoStats
import zed.rainxch.details.domain.repository.DetailsRepository

class DetailsRepositoryImpl(
    private val httpClient: HttpClient,
    private val localizationManager: LocalizationManager,
    private val logger: GitHubStoreLogger
) : DetailsRepository {

    private val readmeHelper = ReadmeLocalizationHelper(localizationManager)

    override suspend fun getRepositoryById(id: Long): GithubRepoSummary {
        val repo = httpClient.executeRequest<RepoByIdNetwork> {
            get("/repositories/$id") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }.getOrThrow()

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

    override suspend fun getLatestPublishedRelease(
        owner: String,
        repo: String,
        defaultBranch: String
    ): GithubRelease? {
        val releases = httpClient.executeRequest<List<ReleaseNetwork>> {
            get("/repos/$owner/$repo/releases") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                parameter("per_page", 10)
            }
        }.getOrNull() ?: return null

        val latest = releases
            .asSequence()
            .filter { (it.draft != true) && (it.prerelease != true) }
            .maxByOrNull { it.publishedAt ?: it.createdAt ?: "" }
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


    override suspend fun getReadme(
        owner: String,
        repo: String,
        defaultBranch: String
    ): Triple<String, String?, String>? {
        val attempts = readmeHelper.generateReadmeAttempts()
        val baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/"
        val primaryLang = localizationManager.getPrimaryLanguageCode()

        logger.debug(
            "Attempting to fetch README for language preference: ${localizationManager.getCurrentLanguageCode()}"
        )

        val foundReadmes = coroutineScope {
            attempts.map { attempt ->
                async(start = CoroutineStart.LAZY) {
                    try {
                        logger.debug("Trying ${attempt.path} (priority: ${attempt.priority})...")

                        val rawMarkdown = httpClient.executeRequest<String> {
                            get("$baseUrl${attempt.path}")
                        }.getOrNull()

                        if (rawMarkdown != null) {
                            logger.debug("Successfully fetched ${attempt.path}")

                            val processed = preprocessMarkdown(
                                markdown = rawMarkdown,
                                baseUrl = baseUrl
                            )

                            val detectedLang = readmeHelper.detectReadmeLanguage(processed)
                            logger.debug("Detected language: ${detectedLang ?: "unknown"} for ${attempt.path}")

                            attempt to Pair(processed, detectedLang)
                        } else {
                            null
                        }
                    } catch (e: Throwable) {
                        logger.debug("Failed to fetch ${attempt.path}: ${e.message}")
                        null
                    }
                }
            }.also { asyncTasks ->
                asyncTasks.take(6).forEach { it.start() }
            }.awaitAll()
                .filterNotNull()
                .associateBy({ it.first }, { it.second })
        }

        if (foundReadmes.isEmpty()) {
            logger.error("Failed to fetch any README variant.")
            return null
        }

        foundReadmes.entries.firstOrNull { (attempt, content) ->
            attempt.filename != "README.md" && content.second == primaryLang
        }?.let { (attempt, content) ->
            logger.debug("Found localized README matching user language: ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        foundReadmes.entries.firstOrNull { (attempt, _) ->
            attempt.filename.contains(".${primaryLang}.", ignoreCase = true) ||
                    attempt.filename.contains("-${primaryLang.uppercase()}.", ignoreCase = true)
        }?.let { (attempt, content) ->
            logger.debug("Found explicit language file for user: ${attempt.path}")
            return Triple(content.first, content.second ?: primaryLang, attempt.path)
        }

        foundReadmes.entries.firstOrNull { (attempt, content) ->
            attempt.filename == "README.md" && content.second == primaryLang
        }?.let { (attempt, content) ->
            logger.debug("Default README matches user language: ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        if (primaryLang == "en") {
            foundReadmes.entries.firstOrNull { (_, content) ->
                content.second == "en"
            }?.let { (attempt, content) ->
                logger.debug("Found English README for English user: ${attempt.path}")
                return Triple(content.first, content.second, attempt.path)
            }
        }

        foundReadmes.entries.firstOrNull { (_, content) ->
            content.second == primaryLang
        }?.let { (attempt, content) ->
            logger.debug("Fallback: Using README matching user language: ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        if (primaryLang == "en") {
            foundReadmes.entries.firstOrNull { (_, content) ->
                content.second == "en"
            }?.let { (attempt, content) ->
                logger.debug("Fallback: Using English README: ${attempt.path}")
                return Triple(content.first, content.second, attempt.path)
            }
        }

        foundReadmes.entries.firstOrNull { (attempt, _) ->
            attempt.path == "README.md"
        }?.let { (attempt, content) ->
            logger.debug("Fallback: Using root README.md (language: ${content.second}): ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        foundReadmes.entries.firstOrNull { (attempt, _) ->
            attempt.path.startsWith(".github/")
        }?.let { (attempt, content) ->
            logger.debug("Fallback: Using .github README: ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        foundReadmes.entries.minByOrNull { it.key.priority }?.let { (attempt, content) ->
            logger.debug("Fallback: Using highest priority README: ${attempt.path}")
            return Triple(content.first, content.second, attempt.path)
        }

        return null
    }

    override suspend fun getRepoStats(owner: String, repo: String): RepoStats {
        val info = httpClient.executeRequest<RepoInfoNetwork> {
            get("/repos/$owner/$repo") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }.getOrThrow()

        return RepoStats(
            stars = info.stars,
            forks = info.forks,
            openIssues = info.openIssues,
        )
    }

    override suspend fun getUserProfile(username: String): GithubUserProfile {
        val user = httpClient.executeRequest<UserProfileNetwork> {
            get("/users/$username") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }.getOrThrow()

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
}