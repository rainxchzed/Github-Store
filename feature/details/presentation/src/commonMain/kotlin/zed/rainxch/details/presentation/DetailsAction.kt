package zed.rainxch.details.presentation

import org.jetbrains.compose.resources.StringResource

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
    data class OpenDeveloperProfile(val username: String) : DetailsAction

    data object OpenInObtainium : DetailsAction
    data object OpenInAppManager : DetailsAction
    data object OnToggleInstallDropdown : DetailsAction

    data object OnNavigateBackClick : DetailsAction

    data object OnToggleFavorite : DetailsAction
    data object CheckForUpdates : DetailsAction
    data object UpdateApp : DetailsAction

    data class OnMessage(val messageText: StringResource) : DetailsAction
}