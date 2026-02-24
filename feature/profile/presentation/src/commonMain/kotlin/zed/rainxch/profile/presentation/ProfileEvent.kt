package zed.rainxch.profile.presentation

sealed interface ProfileEvent {
    data object OnLogoutSuccessful : ProfileEvent
    data class OnLogoutError(val message: String) : ProfileEvent
}