package zed.rainxch.githubstore.feature.details.presentation

sealed interface DetailsAction {
    data object Retry : DetailsAction
    data object InstallPrimary : DetailsAction
    data class DownloadAsset(
        val downloadUrl: String,
        val assetName: String,
        val sizeBytes: Long
    ) : DetailsAction

    data object CancelCurrentDownload : DetailsAction

    data object OpenRepoInBrowser : DetailsAction
    data object OpenAuthorInBrowser : DetailsAction
    data class OpenAuthorInApp(val authorLogin: String) : DetailsAction

    data object OpenInObtainium : DetailsAction
    data object OpenInAppManager : DetailsAction
    data object OnToggleInstallDropdown : DetailsAction

    data object OnNavigateBackClick : DetailsAction

    // NEW ACTIONS
    data object OnToggleFavorite : DetailsAction
    data object CheckForUpdates : DetailsAction
    data object UpdateApp : DetailsAction
}