package zed.rainxch.details.presentation

import zed.rainxch.core.domain.model.SystemArchitecture
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.details.domain.model.ReleaseCategory
import zed.rainxch.details.domain.model.RepoStats
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.details.presentation.model.InstallLogItem

data class DetailsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val repository: GithubRepoSummary? = null,
    val selectedRelease: GithubRelease? = null,
    val installableAssets: List<GithubAsset> = emptyList(),
    val primaryAsset: GithubAsset? = null,
    val userProfile: GithubUserProfile? = null,

    val allReleases: List<GithubRelease> = emptyList(),
    val selectedReleaseCategory: ReleaseCategory = ReleaseCategory.STABLE,
    val isVersionPickerVisible: Boolean = false,

    val stats: RepoStats? = null,
    val readmeMarkdown: String? = null,
    val readmeLanguage: String? = null,

    val installLogs: List<InstallLogItem> = emptyList(),

    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val isInstalling: Boolean = false,
    val downloadError: String? = null,
    val installError: String? = null,

    val downloadStage: DownloadStage = DownloadStage.IDLE,
    val systemArchitecture: SystemArchitecture = SystemArchitecture.UNKNOWN,

    val isObtainiumAvailable: Boolean = false,
    val isObtainiumEnabled: Boolean = false,

    val isInstallDropdownExpanded: Boolean = false,

    val isAppManagerAvailable: Boolean = false,
    val isAppManagerEnabled: Boolean = false,

    val installedApp: InstalledApp? = null,
    val isFavourite: Boolean = false,
    val isStarred: Boolean = false,
    val isTrackingApp: Boolean = false,

    val isAboutExpanded: Boolean = false,
    val isWhatsNewExpanded: Boolean = false,
) {
    /**
     * True when the app is detected as installed on the system (via assets matching)
     * but is NOT yet tracked in our database. Shows the "Track this app" button.
     */
    val isTrackable: Boolean
        get() = installedApp == null &&
                !isLoading &&
                repository != null &&
                primaryAsset != null

    val filteredReleases: List<GithubRelease>
        get() = when (selectedReleaseCategory) {
            ReleaseCategory.STABLE -> allReleases.filter { !it.isPrerelease }
            ReleaseCategory.PRE_RELEASE -> allReleases.filter { it.isPrerelease }
            ReleaseCategory.ALL -> allReleases
        }
}
