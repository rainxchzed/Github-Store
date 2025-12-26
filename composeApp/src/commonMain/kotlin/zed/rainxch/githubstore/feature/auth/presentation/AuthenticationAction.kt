package zed.rainxch.githubstore.feature.auth.presentation

import zed.rainxch.githubstore.core.domain.model.DeviceStart

sealed interface AuthenticationAction {
    data object StartLogin : AuthenticationAction
    data class CopyCode(val start: DeviceStart) : AuthenticationAction
    data class OpenPlatform(val start: DeviceStart) : AuthenticationAction
    data object MarkLoggedOut : AuthenticationAction
    data object MarkLoggedIn : AuthenticationAction
    data class OnInfo(val message: String) : AuthenticationAction
}