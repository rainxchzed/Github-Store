package zed.rainxch.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.domain.repository.AuthenticationState

class AuthenticationStateImpl (
    private val tokenStore: TokenStore,
) : AuthenticationState {

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    override fun isUserLoggedIn(): Flow<Boolean> {
        return tokenStore
            .tokenFlow()
            .map {
                it != null
            }
    }

    override suspend fun isCurrentlyUserLoggedIn(): Boolean {
        return tokenStore.currentToken() != null
    }

    override suspend fun notifySessionExpired() {
        tokenStore.clear()
        _sessionExpiredEvent.emit(Unit)
    }
}