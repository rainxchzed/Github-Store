package zed.rainxch.githubstore.feature.details.presentation

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

    data object OnToggleFavorite : DetailsAction
    data object CheckForUpdates : DetailsAction
    data object UpdateApp : DetailsAction
    data object OpenRepoInBrowser : DetailsAction
    data object OpenAuthorInBrowser : DetailsAction
    data object OpenInObtainium : DetailsAction
    data object OpenInAppManager : DetailsAction
    data object OnToggleInstallDropdown : DetailsAction

    // Shizuku actions
    data object RequestShizukuPermission : DetailsAction

    data object OpenShizukuSetupDialog : DetailsAction
    data object CloseShizukuSetupDialog : DetailsAction
    data object OpenShizukuApp : DetailsAction
    data object RefreshShizukuStatus : DetailsAction
    data object OnShizukuRequestPermission : DetailsAction

    // Navigation actions (handled in composable)
    data object OnNavigateBackClick : DetailsAction
    data class OpenDeveloperProfile(val username: String) : DetailsAction
    data class OnMessage(val messageText: StringResource) : DetailsAction
}
