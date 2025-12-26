package zed.rainxch.githubstore.feature.settings.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val tokenDataSource: TokenDataSource,
) : SettingsRepository {
    override val isGithubUserLoggedIn: Flow<Boolean>
        get() = tokenDataSource
            .tokenFlow(ApiPlatform.Github)
            .map {
                it != null
            }
            .flowOn(Dispatchers.IO)

    override val isGitlabUserLoggedIn: Flow<Boolean>
        get() = tokenDataSource
            .tokenFlow(ApiPlatform.GitLab)
            .map {
                it != null
            }
            .flowOn(Dispatchers.IO)

    override suspend fun logout(apiPlatform: ApiPlatform) {
        tokenDataSource.clear(apiPlatform)
    }
}