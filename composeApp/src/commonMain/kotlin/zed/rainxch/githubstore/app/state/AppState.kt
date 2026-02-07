package zed.rainxch.githubstore.app.state

import zed.rainxch.core.domain.model.RateLimitInfo
import zed.rainxch.core.domain.model.AppTheme

data class AppState(
    val rateLimitInfo: RateLimitInfo? = null,
    val showRateLimitDialog: Boolean = false,
    val isLoggedIn: Boolean = false,
    val theme: AppTheme = AppTheme.OCEAN
)
