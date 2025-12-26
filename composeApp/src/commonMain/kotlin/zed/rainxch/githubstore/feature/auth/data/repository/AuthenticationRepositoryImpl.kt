package zed.rainxch.githubstore.feature.auth.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess
import zed.rainxch.githubstore.feature.auth.data.getGitLabClientId
import zed.rainxch.githubstore.feature.auth.data.network.GitHubAuthApi
import zed.rainxch.githubstore.feature.auth.data.getGithubClientId
import zed.rainxch.githubstore.feature.auth.data.network.GitLabAuthApi
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthenticationRepository

class AuthenticationRepositoryImpl(
    private val tokenDataSource: TokenDataSource,
    private val apiPlatform: ApiPlatform,
    private val gitlabUrl: String = "https://gitlab.com"
) : AuthenticationRepository {

    override val accessTokenFlow: Flow<String?>
        get() = tokenDataSource.tokenFlow(apiPlatform).map { it?.accessToken }

    override val isAuthenticatedFlow: Flow<Boolean>
        get() = tokenDataSource.tokenFlow(apiPlatform).map { it != null }

    override suspend fun isAuthenticated(): Boolean =
        tokenDataSource.current() != null

    override suspend fun startDeviceFlow(): DeviceStart =
        withContext(Dispatchers.IO) {
            when (apiPlatform) {
                ApiPlatform.Github -> {
                    val clientId = getGithubClientId()
                    require(clientId.isNotBlank()) {
                        "Missing GitHub CLIENT_ID. Add GITHUB_CLIENT_ID to local.properties."
                    }

                    try {
                        val result = GitHubAuthApi.startDeviceFlow(clientId)
                        Logger.d { "✅ GitHub device flow started. User code: ${result.userCode}" }
                        result
                    } catch (e: Exception) {
                        Logger.d { "❌ Failed to start GitHub device flow: ${e.message}" }
                        throw Exception(
                            "Failed to start GitHub authentication. " +
                                    "Please check your internet connection and try again.",
                            e
                        )
                    }
                }

                ApiPlatform.GitLab -> {
                    val clientId = getGitLabClientId()
                    require(clientId.isNotBlank()) {
                        "Missing GitLab Application ID. Add GITLAB_CLIENT_ID to local.properties."
                    }

                    try {
                        val scopes = "api"
                        val result = GitLabAuthApi.startDeviceFlow(clientId, gitlabUrl, scopes)
                        Logger.d { "✅ GitLab device flow started. User code: ${result.userCode}" }
                        result
                    } catch (e: Exception) {
                        Logger.d { "❌ Failed to start GitLab device flow: ${e.message}" }
                        throw Exception(
                            "Failed to start GitLab authentication. " +
                                    "Please check your internet connection and try again.",
                            e
                        )
                    }
                }
            }
        }

    override suspend fun awaitDeviceToken(start: DeviceStart): DeviceTokenSuccess =
        withContext(Dispatchers.IO) {
            val clientId = when (apiPlatform) {
                ApiPlatform.Github -> getGithubClientId()
                ApiPlatform.GitLab -> getGitLabClientId()
            }

            val timeoutMs = start.expiresInSec * 1000L
            var remainingMs = timeoutMs

            val initialJitter = (0..2000).random().toLong()
            delay(initialJitter)
            remainingMs -= initialJitter

            var intervalMs = (start.intervalSec.coerceAtLeast(5)) * 1000L
            var consecutiveErrors = 0
            var slowDownCount = 0
            val maxConsecutiveErrors = 5
            val maxSlowDownBeforeGiveUp = 8

            Logger.d { "⏱️ Starting token polling for ${apiPlatform.name}. Expires in: ${start.expiresInSec}s, Interval: ${start.intervalSec}s (jitter: ${initialJitter}ms)" }

            while (remainingMs > 0) {
                try {
                    val res = when (apiPlatform) {
                        ApiPlatform.Github -> GitHubAuthApi.pollDeviceToken(clientId, start.deviceCode)
                        ApiPlatform.GitLab -> GitLabAuthApi.pollDeviceToken(clientId, start.deviceCode, gitlabUrl)
                    }
                    val success = res.getOrNull()

                    if (success != null) {
                        Logger.d { "✅ Token received successfully from ${apiPlatform.name}!" }
                        withRetry(maxAttempts = 3) {
                            tokenDataSource.save(apiPlatform, success)
                        }
                        return@withContext success
                    }

                    val error = res.exceptionOrNull()
                    val msg = (error?.message ?: "").lowercase()

                    Logger.d { "📡 Poll response: $msg (slowdowns: $slowDownCount/$maxSlowDownBeforeGiveUp)" }

                    when {
                        "authorization_pending" in msg -> {
                            consecutiveErrors = 0
                            slowDownCount = 0

                            val jitter = (0..500).random().toLong()
                            val waitTime = intervalMs + jitter

                            delay(waitTime)
                            remainingMs -= waitTime
                        }

                        "slow_down" in msg -> {
                            consecutiveErrors = 0
                            slowDownCount++

                            intervalMs += 5000

                            Logger.d { "⚠️ Rate limited. Slowing to ${intervalMs}ms (slowdown #$slowDownCount)" }

                            if (slowDownCount >= maxSlowDownBeforeGiveUp) {
                                throw Exception(
                                    "${apiPlatform.name} authentication is experiencing high traffic. " +
                                            "Please wait 1-2 minutes and try again."
                                )
                            }

                            val extraJitter = (0..3000).random().toLong()
                            val waitTime = intervalMs + extraJitter

                            Logger.d { "⏳ Waiting ${waitTime}ms (base: ${intervalMs}ms + jitter: ${extraJitter}ms)" }

                            delay(waitTime)
                            remainingMs -= waitTime
                        }

                        "access_denied" in msg -> {
                            throw CancellationException("You denied access to the app")
                        }

                        "expired_token" in msg || "expired_device_code" in msg -> {
                            throw CancellationException(
                                "Authorization timed out. Please try again."
                            )
                        }

                        "unable to resolve" in msg ||
                                "no address" in msg ||
                                "failed to connect" in msg ||
                                "connection refused" in msg ||
                                "network is unreachable" in msg -> {
                            consecutiveErrors++
                            Logger.d { "⚠️ Network error, retrying... ($consecutiveErrors/$maxConsecutiveErrors)" }

                            if (consecutiveErrors >= maxConsecutiveErrors) {
                                throw Exception(
                                    "Network connection unstable during authentication. " +
                                            "Please check your connection and try again."
                                )
                            }

                            val backoffDelay = intervalMs * (1 + consecutiveErrors)
                            delay(backoffDelay)
                            remainingMs -= backoffDelay
                        }

                        else -> {
                            consecutiveErrors++
                            Logger.d { "⚠️ Unknown error: $msg (attempt $consecutiveErrors/$maxConsecutiveErrors)" }

                            if (consecutiveErrors >= maxConsecutiveErrors) {
                                throw Exception("Authentication failed: $msg")
                            }

                            val backoffDelay = intervalMs * 2
                            delay(backoffDelay)
                            remainingMs -= backoffDelay
                        }
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.d { "❌ Poll error: ${e.message}" }
                    consecutiveErrors++

                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        throw Exception(
                            "Authentication failed after multiple attempts. " +
                                    "Error: ${e.message}",
                            e
                        )
                    }

                    val backoffDelay = intervalMs * (1 + consecutiveErrors)
                    delay(backoffDelay)
                    remainingMs -= backoffDelay
                }
            }

            throw CancellationException(
                "Authentication timed out. Please try again and complete the process faster."
            )
        }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> T
    ): T {
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Logger.d { "⚠️ Retry attempt ${attempt + 1} failed: ${e.message}" }
                delay(initialDelay * (attempt + 1))
            }
        }
        return block()
    }
}