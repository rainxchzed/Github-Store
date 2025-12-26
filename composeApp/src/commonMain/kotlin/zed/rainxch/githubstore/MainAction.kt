package zed.rainxch.githubstore

import zed.rainxch.githubstore.core.domain.model.ApiPlatform

sealed interface MainAction {
    data object DismissRateLimitDialog : MainAction
    data class SwitchPlatform(val platform: ApiPlatform) : MainAction
}