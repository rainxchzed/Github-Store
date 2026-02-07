package zed.rainxch.settings.presentation

sealed interface SettingsEvent {
    data object OnLogoutSuccessful : SettingsEvent
    data class OnLogoutError(val message: String) : SettingsEvent
}