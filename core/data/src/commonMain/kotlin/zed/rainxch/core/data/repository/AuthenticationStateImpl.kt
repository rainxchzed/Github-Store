package zed.rainxch.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.domain.repository.AuthenticationState

class AuthenticationStateImpl (
    private val tokenStore: TokenStore,
) : AuthenticationState {
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
}