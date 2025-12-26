package zed.rainxch.githubstore.app.app_state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.network.RateLimitHandler
import zed.rainxch.githubstore.network.RateLimitInfo
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AppStateManager(
    val rateLimitHandler: RateLimitHandler,
    val githubTokenDataSource: TokenDataSource,
    val gitlabTokenDataSource: TokenDataSource
) {
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                githubTokenDataSource.tokenFlow(ApiPlatform.Github).collect { token ->
                    val isAuth = token != null
                    _appState.update { it.copy(isGithubAuthenticated = isAuth) }

                    if (isAuth) {
                        rateLimitHandler.clearRateLimit(ApiPlatform.Github)
                        updateRateLimit(null, ApiPlatform.Github)
                    }
                }
            }

            launch {
                gitlabTokenDataSource.tokenFlow(ApiPlatform.GitLab).collect { token ->
                    val isAuth = token != null
                    _appState.update { it.copy(isGitlabAuthenticated = isAuth) }

                    if (isAuth) {
                        rateLimitHandler.clearRateLimit(ApiPlatform.GitLab)
                        updateRateLimit(null, ApiPlatform.GitLab)
                    }
                }
            }
        }
    }

    fun updateRateLimit(rateLimitInfo: RateLimitInfo?, apiPlatform: ApiPlatform) {
        _appState.update { currentState ->
            val shouldShowDialog = if (rateLimitInfo?.isExhausted == true) {
                true
            } else {
                currentState.showRateLimitDialog && currentState.rateLimitDialogPlatform == apiPlatform
            }

            when (apiPlatform) {
                ApiPlatform.Github -> currentState.copy(
                    githubRateLimitInfo = rateLimitInfo,
                    showRateLimitDialog = shouldShowDialog,
                    rateLimitDialogPlatform = if (shouldShowDialog) apiPlatform else currentState.rateLimitDialogPlatform
                )

                ApiPlatform.GitLab -> currentState.copy(
                    gitlabRateLimitInfo = rateLimitInfo,
                    showRateLimitDialog = shouldShowDialog,
                    rateLimitDialogPlatform = if (shouldShowDialog) apiPlatform else currentState.rateLimitDialogPlatform
                )
            }
        }
    }

    fun dismissRateLimitDialog() {
        _appState.update { it.copy(showRateLimitDialog = false, rateLimitDialogPlatform = null) }
    }

    @OptIn(ExperimentalTime::class)
    fun triggerAuthDialog(apiPlatform: ApiPlatform) {
        _appState.update {
            it.copy(
                showRateLimitDialog = true,  // Reuse rate limit dialog for auth prompt
                rateLimitDialogPlatform = apiPlatform,
                // Optionally set a fake RateLimitInfo with remaining=0 to force "exhausted" look
                githubRateLimitInfo = if (apiPlatform == ApiPlatform.Github) RateLimitInfo(
                    limit = 0,
                    remaining = 0,
                    reset = Instant.DISTANT_FUTURE,
                    apiPlatform = apiPlatform
                ) else it.githubRateLimitInfo,
                gitlabRateLimitInfo = if (apiPlatform == ApiPlatform.GitLab) RateLimitInfo(
                    limit = 0,
                    remaining = 0,
                    reset = Instant.DISTANT_FUTURE,
                    apiPlatform = apiPlatform
                ) else it.gitlabRateLimitInfo
            )
        }
    }
}