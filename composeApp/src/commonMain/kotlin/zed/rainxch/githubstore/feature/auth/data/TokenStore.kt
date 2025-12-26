package zed.rainxch.githubstore.feature.auth.data

import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess

interface TokenStore {
    suspend fun save(apiPlatform: ApiPlatform, token: DeviceTokenSuccess)
    suspend fun load(apiPlatform: ApiPlatform): DeviceTokenSuccess?
    suspend fun clear(apiPlatform: ApiPlatform)
}

expect fun getGithubClientId(): String
expect fun getGitLabClientId(): String

expect fun getGitLabClientSecret(): String