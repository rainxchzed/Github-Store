package zed.rainxch.githubstore.app.app_state

import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.network.RateLimitInfo

data class AppState(
    val githubRateLimitInfo: RateLimitInfo? = null,
    val gitlabRateLimitInfo: RateLimitInfo? = null,
    val isGithubAuthenticated: Boolean = false,
    val isGitlabAuthenticated: Boolean = false,
    val rateLimitDialogPlatform: ApiPlatform? = null,
    val showRateLimitDialog: Boolean = false
) {
    fun getRateLimitInfo(apiPlatform: ApiPlatform): RateLimitInfo? = when (apiPlatform) {
        ApiPlatform.Github -> githubRateLimitInfo
        ApiPlatform.GitLab -> gitlabRateLimitInfo
    }

    fun isAuthenticated(apiPlatform: ApiPlatform): Boolean = when (apiPlatform) {
        ApiPlatform.Github -> isGithubAuthenticated
        ApiPlatform.GitLab -> isGitlabAuthenticated
    }
}
