package zed.rainxch.settings.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.domain.repository.AuthenticationState
import zed.rainxch.feature.settings.data.BuildKonfig
import zed.rainxch.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val authenticationState: AuthenticationState,
    private val tokenStore: TokenStore
) : SettingsRepository {
    override val isUserLoggedIn: Flow<Boolean>
        get() = authenticationState
            .isUserLoggedIn()
            .flowOn(Dispatchers.IO)

    override fun getVersionName(): String {
        return BuildKonfig.VERSION_NAME
    }

    override suspend fun logout() {
        tokenStore.clear()
    }
}