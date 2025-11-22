package zed.rainxch.githubstore.feature.auth.presentation

sealed interface AuthenticationEvents {
    data class OpenBrowser(val url: String) : AuthenticationEvents
    data class CopyToClipboard(val label: String, val text: String) : AuthenticationEvents
    data object OnNavigateToMain : AuthenticationEvents
}