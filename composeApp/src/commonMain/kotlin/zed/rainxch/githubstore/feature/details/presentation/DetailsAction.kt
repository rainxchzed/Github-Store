package zed.rainxch.githubstore.feature.details.presentation

sealed interface DetailsAction {
    data object OnNavigateBackClick : DetailsAction

    data object Retry : DetailsAction

    data object InstallPrimary : DetailsAction
    data class DownloadAsset(val assetName: String, val downloadUrl: String, val sizeBytes: Long) : DetailsAction
    data object CancelCurrentDownload : DetailsAction

    data object OpenRepoInBrowser : DetailsAction
    data object OpenAuthorInBrowser : DetailsAction
    data class OpenAuthorInApp(val authorId: Int) : DetailsAction
}