package zed.rainxch.settings.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val tokenDataSource: TokenStore,
) : SettingsRepository {
    override val isUserLoggedIn: Flow<Boolean>
        get() = tokenDataSource
            .tokenFlow()
            .map {
                it != null
            }
            .flowOn(Dispatchers.IO)

    override suspend fun logout() {
        tokenDataSource.clear()
    }
}