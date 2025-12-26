package zed.rainxch.githubstore

import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.presentation.model.AppTheme
import zed.rainxch.githubstore.network.RateLimitInfo

data class MainState(
    val isCheckingAuth: Boolean = true,
    val isGithubLoggedIn: Boolean = false,
    val isGitlabLoggedIn: Boolean = false,
    val githubRateLimitInfo: RateLimitInfo? = null,
    val gitlabRateLimitInfo: RateLimitInfo? = null,
    val showRateLimitDialog: Boolean = false,
    val rateLimitDialogPlatform: ApiPlatform? = null,
    val currentColorTheme: AppTheme = AppTheme.OCEAN,
    val isAmoledTheme: Boolean = false,
    val currentApiPlatform: ApiPlatform = ApiPlatform.Github
)
