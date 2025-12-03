package zed.rainxch.githubstore.feature.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess

interface AuthRepository {
    val accessTokenFlow: Flow<String?>

    suspend fun startDeviceFlow(scope: String): DeviceStart

    suspend fun awaitDeviceToken(start: DeviceStart): DeviceTokenSuccess

    suspend fun logout()
}