package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthenticationState {
    fun isUserLoggedIn() : Flow<Boolean>
    suspend fun isCurrentlyUserLoggedIn() : Boolean
}