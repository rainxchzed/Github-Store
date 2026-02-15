package zed.rainxch.githubstore

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.RateLimitInfo

data class MainState(
    val isLoggedIn: Boolean = false,
    val rateLimitInfo: RateLimitInfo? = null,
    val showRateLimitDialog: Boolean = false,
    val currentColorTheme: AppTheme = AppTheme.OCEAN,
    val isAmoledTheme: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val currentFontTheme: FontTheme = FontTheme.CUSTOM,
)
