package zed.rainxch.githubstore.feature.auth.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.core.data.TokenDataSource
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess
import zed.rainxch.githubstore.feature.auth.data.network.GitHubAuthApi
import zed.rainxch.githubstore.feature.auth.data.getGithubClientId
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val tokenDataSource: TokenDataSource,
    private val scopeText: String = DEFAULT_SCOPE
) : AuthRepository {

    override val accessTokenFlow: Flow<String?>
        get() = tokenDataSource.tokenFlow.map { it?.accessToken }

    override suspend fun startDeviceFlow(scope: String): DeviceStart =
        withContext(Dispatchers.Default) {
            val clientId = getGithubClientId()
            require(clientId.isNotBlank()) {
                "Missing GitHub CLIENT_ID. Add GITHUB_CLIENT_ID to local.properties."
            }

            try {
                val result = GitHubAuthApi.startDeviceFlow(clientId, scope.ifBlank { scopeText })
                Logger.d { "✅ Device flow started. User code: ${result.userCode}" }
                result
            } catch (e: Exception) {
                Logger.d { "❌ Failed to start device flow: ${e.message}" }
                throw Exception(
                    "Failed to start GitHub authentication. " +
                            "Please check your internet connection and try again.",
                    e
                )
            }
        }

    override suspend fun awaitDeviceToken(start: DeviceStart): DeviceTokenSuccess =
        withContext(Dispatchers.Default) {
            val clientId = getGithubClientId()
            val timeoutMs = start.expiresInSec * 1000L
            var remainingMs = timeoutMs
            var intervalMs = (start.intervalSec.coerceAtLeast(5)) * 1000L
            var consecutiveErrors = 0
            val maxConsecutiveErrors = 3

            Logger.d { "⏱️ Starting token polling. Expires in: ${start.expiresInSec}s, Interval: ${start.intervalSec}s" }

            while (remainingMs > 0) {
                try {
                    val res = GitHubAuthApi.pollDeviceToken(clientId, start.deviceCode)
                    val success = res.getOrNull()

                    if (success != null) {
                        Logger.d { "✅ Token received successfully!" }
                        tokenDataSource.save(success)
                        return@withContext success
                    }

                    val error = res.exceptionOrNull()
                    val msg = (error?.message ?: "").lowercase()

                    Logger.d { "📡 Poll response: $msg" }

                    when {
                        "authorization_pending" in msg -> {
                            consecutiveErrors = 0
                            delay(intervalMs)
                            remainingMs -= intervalMs
                        }

                        "slow_down" in msg -> {
                            consecutiveErrors = 0
                            intervalMs += 5000
                            Logger.d { "⚠️ Slowing down polling to ${intervalMs}ms" }
                            delay(intervalMs)
                            remainingMs -= intervalMs
                        }

                        "access_denied" in msg -> {
                            throw CancellationException("You denied access to the app")
                        }

                        "expired_token" in msg || "expired_device_code" in msg -> {
                            throw CancellationException(
                                "Authorization timed out. Please try again."
                            )
                        }

                        "unable to resolve" in msg || "no address" in msg -> {
                            consecutiveErrors++
                            if (consecutiveErrors >= maxConsecutiveErrors) {
                                throw Exception(
                                    "Network connection lost during authentication. " +
                                            "Please check your connection and try again."
                                )
                            }
                            Logger.d { "⚠️ Network error, retrying... ($consecutiveErrors/$maxConsecutiveErrors)" }
                            delay(intervalMs)
                            remainingMs -= intervalMs
                        }

                        else -> {
                            consecutiveErrors++
                            if (consecutiveErrors >= maxConsecutiveErrors) {
                                throw Exception("Authentication failed: $msg")
                            }
                            Logger.d { "⚠️ Unknown error, retrying... ($consecutiveErrors/$maxConsecutiveErrors)" }
                            delay(intervalMs)
                            remainingMs -= intervalMs
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
                                    "Please try again.",
                            e
                        )
                    }
                    delay(intervalMs)
                    remainingMs -= intervalMs
                }
            }

            throw CancellationException(
                "Authentication timed out. Please try again and complete the process faster."
            )
        }

    override suspend fun logout() {
        tokenDataSource.clear()
    }

    companion object {
        const val DEFAULT_SCOPE = "read:user repo"
    }
}