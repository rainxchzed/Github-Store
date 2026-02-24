package zed.rainxch.profile.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme

sealed interface ProfileAction {
    data object OnNavigateBackClick : ProfileAction
    data class OnThemeColorSelected(val themeColor: AppTheme) : ProfileAction
    data class OnAmoledThemeToggled(val enabled: Boolean) : ProfileAction
    data class OnDarkThemeChange(val isDarkTheme: Boolean?) : ProfileAction
    data object OnLogoutClick : ProfileAction
    data object OnLogoutConfirmClick : ProfileAction
    data object OnLogoutDismiss : ProfileAction
    data object OnHelpClick : ProfileAction
    data class OnFontThemeSelected(val fontTheme: FontTheme) : ProfileAction
}