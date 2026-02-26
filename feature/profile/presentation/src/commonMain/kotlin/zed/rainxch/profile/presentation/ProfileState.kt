package zed.rainxch.profile.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.profile.domain.model.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val selectedThemeColor: AppTheme = AppTheme.OCEAN,
    val selectedFontTheme: FontTheme = FontTheme.CUSTOM,
    val isLogoutDialogVisible: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val isAmoledThemeEnabled: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val versionName: String = "",
    val proxyType: ProxyType = ProxyType.NONE,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val isProxyPasswordVisible: Boolean = false,
)

enum class ProxyType {
    NONE, SYSTEM, HTTP, SOCKS;

    companion object {
        fun fromConfig(config: ProxyConfig): ProxyType = when (config) {
            is ProxyConfig.None -> NONE
            is ProxyConfig.System -> SYSTEM
            is ProxyConfig.Http -> HTTP
            is ProxyConfig.Socks -> SOCKS
        }
    }
}