package zed.rainxch.settings.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme

data class SettingsState(
    val selectedThemeColor: AppTheme = AppTheme.OCEAN,
    val selectedFontTheme: FontTheme = FontTheme.CUSTOM,
    val isLogoutDialogVisible: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val isAmoledThemeEnabled: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val versionName: String = ""
)