package zed.rainxch.githubstore.feature.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.core.domain.model.ApiPlatform

interface SettingsRepository {
    val isGitlabUserLoggedIn: Flow<Boolean>
    val isGithubUserLoggedIn: Flow<Boolean>

    suspend fun logout(apiPlatform: ApiPlatform)
}