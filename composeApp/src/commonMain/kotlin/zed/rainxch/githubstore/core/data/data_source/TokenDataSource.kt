@file:OptIn(ExperimentalTime::class)

package zed.rainxch.githubstore.core.data.data_source

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess
import zed.rainxch.githubstore.feature.auth.data.TokenStore
import zed.rainxch.githubstore.feature.auth.data.getGitLabClientId
import zed.rainxch.githubstore.feature.auth.data.getGitLabClientSecret
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface TokenDataSource {
    fun tokenFlow(apiPlatform: ApiPlatform): StateFlow<DeviceTokenSuccess?>
    suspend fun save(apiPlatform: ApiPlatform, token: DeviceTokenSuccess)
    suspend fun reloadFromStore(apiPlatform: ApiPlatform): DeviceTokenSuccess?
    suspend fun clear(apiPlatform: ApiPlatform)
    suspend fun refreshIfNeeded(apiPlatform: ApiPlatform): DeviceTokenSuccess?
    fun current(): DeviceTokenSuccess?
}

interface TokenRefresher {
    suspend fun refresh(apiPlatform: ApiPlatform, refreshToken: String): DeviceTokenSuccess
}

@OptIn(ExperimentalAtomicApi::class)
class DefaultTokenDataSource(
    private val tokenStore: TokenStore,
    private val apiPlatform: ApiPlatform,
    private val tokenRefresher: TokenRefresher,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : TokenDataSource {
    private val _flow = MutableStateFlow<DeviceTokenSuccess?>(null)
    private val isInitialized = CompletableDeferred<Unit>()
    private val refreshMutex = Mutex()

    override fun tokenFlow(apiPlatform: ApiPlatform): StateFlow<DeviceTokenSuccess?> {
        return _flow
    }

    init {
        scope.launch {
            try {
                val token = tokenStore.load(apiPlatform)
                _flow.value = token
            } finally {
                isInitialized.complete(Unit)
            }
        }
    }

    override suspend fun save(apiPlatform: ApiPlatform, token: DeviceTokenSuccess) {
        tokenStore.save(apiPlatform = apiPlatform, token = token)
        _flow.value = token
    }

    override suspend fun reloadFromStore(apiPlatform: ApiPlatform): DeviceTokenSuccess? {
        isInitialized.await()
        return _flow.value
    }

    override suspend fun clear(apiPlatform: ApiPlatform) {
        tokenStore.clear(apiPlatform = apiPlatform)
        _flow.value = null
    }

    override suspend fun refreshIfNeeded(apiPlatform: ApiPlatform): DeviceTokenSuccess? {
        refreshMutex.withLock {
            val token = _flow.value ?: return null

            if (apiPlatform == ApiPlatform.Github) return token

            if (!token.isExpiredOrExpiringSoon()) return token

            val refreshToken = token.refreshToken ?: run {
                Logger.e { "Token expired but no refresh token available" }
                return null
            }

            return try {
                Logger.d { "Refreshing GitLab token..." }
                val newToken = tokenRefresher.refresh(apiPlatform, refreshToken)
                save(apiPlatform, newToken)
                Logger.d { "Token refreshed successfully" }
                newToken
            } catch (e: Exception) {
                Logger.e { "Failed to refresh token: ${e.message}" }
                clear(apiPlatform)
                null
            }
        }
    }

    override fun current(): DeviceTokenSuccess? = _flow.value
}

class OAuthTokenRefresher(
    private val httpClient: HttpClient
) : TokenRefresher {
    override suspend fun refresh(
        apiPlatform: ApiPlatform,
        refreshToken: String
    ): DeviceTokenSuccess {
        return when (apiPlatform) {
            ApiPlatform.Github -> throw IllegalStateException("GitHub tokens don't need refresh")
            ApiPlatform.GitLab -> refreshGitLabToken(refreshToken)
        }
    }

    private suspend fun refreshGitLabToken(refreshToken: String): DeviceTokenSuccess {
        val response = httpClient.post("https://gitlab.com/oauth/token") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken,
                    "client_id" to getGitLabClientId(),
                    "client_secret" to getGitLabClientSecret()
                )
            )
        }

        if (!response.status.isSuccess()) {
            throw Exception("Token refresh failed: ${response.status}")
        }

        val newToken = response.body<DeviceTokenSuccess>()
        return newToken.copy(
            platform = ApiPlatform.GitLab,
            createdAt = Clock.System.now().epochSeconds
        )
    }
}