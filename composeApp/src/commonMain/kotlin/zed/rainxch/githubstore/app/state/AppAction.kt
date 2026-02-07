package zed.rainxch.githubstore.app.state

sealed interface AppAction {
    data object DismissRateLimitDialog : AppAction
}
