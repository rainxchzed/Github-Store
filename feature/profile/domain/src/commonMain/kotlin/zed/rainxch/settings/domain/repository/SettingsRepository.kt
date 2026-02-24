package zed.rainxch.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isUserLoggedIn: Flow<Boolean>
    suspend fun logout()
    fun getVersionName() : String
}