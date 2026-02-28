package zed.rainxch.details.presentation

import org.jetbrains.compose.resources.StringResource
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.details.domain.model.ReleaseCategory

sealed interface DetailsAction {
    data object Retry : DetailsAction
    data object InstallPrimary : DetailsAction
    data object UninstallApp : DetailsAction
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
    data object OnShareClick : DetailsAction
    data object UpdateApp : DetailsAction
    data object OpenApp : DetailsAction

    data class OnMessage(val messageText: StringResource) : DetailsAction

    data class SelectReleaseCategory(val category: ReleaseCategory) : DetailsAction
    data class SelectRelease(val release: GithubRelease) : DetailsAction
    data object ToggleVersionPicker : DetailsAction
    data object ToggleAboutExpanded : DetailsAction
    data object ToggleWhatsNewExpanded : DetailsAction
}