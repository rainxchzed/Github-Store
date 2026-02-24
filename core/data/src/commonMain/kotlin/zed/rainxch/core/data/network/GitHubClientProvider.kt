package zed.rainxch.core.data.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.repository.RateLimitRepository

class GitHubClientProvider(
    private val tokenStore: TokenStore,
    private val rateLimitRepository: RateLimitRepository,
    proxyConfigFlow: Flow<ProxyConfig?>
) {
    private val _client = MutableStateFlow<HttpClient?>(null)

    val client: Flow<HttpClient> = proxyConfigFlow
        .distinctUntilChanged()
        .map { proxyConfig ->
            _client.value?.close()

            val newClient = createGitHubHttpClient(
                tokenStore = tokenStore,
                rateLimitRepository = rateLimitRepository,
                proxyConfig = proxyConfig
            )
            _client.value = newClient
            newClient
        }
        .stateIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            started = SharingStarted.Lazily,
            initialValue = createGitHubHttpClient(tokenStore, rateLimitRepository)
        )

    fun currentClient(): HttpClient {
        return _client.value
            ?: createGitHubHttpClient(tokenStore, rateLimitRepository).also {
                _client.value = it
            }
    }

    fun close() {
        _client.value?.close()
    }
}