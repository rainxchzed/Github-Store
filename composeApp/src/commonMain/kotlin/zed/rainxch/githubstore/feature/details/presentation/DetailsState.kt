package zed.rainxch.githubstore.feature.details.presentation

import zed.rainxch.githubstore.core.data.local.db.entities.InstalledApp
import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.core.domain.model.GithubRelease
import zed.rainxch.githubstore.core.domain.model.GithubAsset
import zed.rainxch.githubstore.core.domain.model.GithubUserProfile
import zed.rainxch.githubstore.feature.details.domain.model.RepoStats
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult

data class InstallLogItem(
    val timeIso: String,
    val assetName: String,
    val assetSizeBytes: Long,
    val releaseTag: String,
    val result: LogResult
)

data class DetailsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val repository: GithubRepoSummary? = null,
    val latestRelease: GithubRelease? = null,
    val installableAssets: List<GithubAsset> = emptyList(),
    val primaryAsset: GithubAsset? = null,
    val userProfile: GithubUserProfile? = null,

    val stats: RepoStats? = null,
    val readmeMarkdown: String? = null,
    val readmeLanguage: String? = null,

    val installLogs: List<InstallLogItem> = emptyList(),

    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val isInstalling: Boolean = false,
    val downloadError: String? = null,
    val installError: String? = null,

    val downloadStage: DownloadStage = DownloadStage.IDLE,
    val systemArchitecture: Architecture = Architecture.UNKNOWN,

    val isObtainiumAvailable: Boolean = false,
    val isObtainiumEnabled: Boolean = false,

    val isInstallDropdownExpanded: Boolean = false,

    val isAppManagerAvailable: Boolean = false,
    val isAppManagerEnabled: Boolean = false,

    val installedApp: InstalledApp? = null,
    val isFavourite: Boolean = false,
    val isStarred: Boolean = false,

    val installProgressPercent: Int? = null,
    val isShizukuAvailable: Boolean = false,
    val isShizukuEnabled: Boolean = false,
    val isShizukuInstalled: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val hasShizukuPermission: Boolean = false,
    val showShizukuSetupDialog: Boolean = false,
)

enum class DownloadStage {
    IDLE, DOWNLOADING, VERIFYING, INSTALLING
}