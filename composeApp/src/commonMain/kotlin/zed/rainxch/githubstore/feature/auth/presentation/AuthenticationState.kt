package zed.rainxch.githubstore.feature.auth.presentation

import zed.rainxch.githubstore.feature.auth.data.DeviceStart

data class AuthenticationState(
    val loginState: AuthLoginState = AuthLoginState.LoggedOut,
    val copied: Boolean = false
)

sealed interface AuthLoginState {
    data object LoggedOut : AuthLoginState
    data class DevicePrompt(
        val start: DeviceStart,
    ) : AuthLoginState

    data object Pending : AuthLoginState
    data object LoggedIn : AuthLoginState
    data class Error(val message: String) : AuthLoginState
}