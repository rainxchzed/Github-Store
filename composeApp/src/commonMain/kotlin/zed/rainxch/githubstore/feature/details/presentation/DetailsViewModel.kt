package zed.rainxch.githubstore.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import zed.rainxch.githubstore.core.presentation.utils.openBrowser
import zed.rainxch.githubstore.feature.details.data.Downloader
import zed.rainxch.githubstore.feature.details.data.Installer
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

class DetailsViewModel(
    private val repositoryId: Int,
    private val detailsRepository: DetailsRepository,
    private val downloader: Downloader,
    private val installer: Installer,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var currentDownloadJob: Job? = null

    private val _state = MutableStateFlow(DetailsState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadInitial()

                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DetailsState()
        )

    private fun loadInitial() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val repo = detailsRepository.getRepositoryById(repositoryId.toLong())
                val owner = repo.owner.login
                val name = repo.name

                _state.value = _state.value.copy(repository = repo)

                val latestReleaseDeferred = async {
                    try {
                        detailsRepository.getLatestPublishedRelease(owner, name)
                    } catch (t: Throwable) {
                        Logger.w { "Failed to load latest release: ${t.message}" }
                        null
                    }
                }

                val statsDeferred = async {
                    try {
                        detailsRepository.getRepoStats(owner, name)
                    } catch (t: Throwable) {
                        null
                    }
                }

                val readmeDeferred = async {
                    try {
                        detailsRepository.getReadme(owner, name)
                    } catch (t: Throwable) {
                        null
                    }
                }

                val userProfileDeferred = async {
                    try {
                        detailsRepository.getUserProfile(owner)
                    } catch (t: Throwable) {
                        Logger.w { "Failed to load user profile: ${t.message}" }
                        null
                    }
                }

                val latestRelease = latestReleaseDeferred.await()
                val stats = statsDeferred.await()
                val readme = readmeDeferred.await()
                val userProfile = userProfileDeferred.await()

                // Get installable assets from installer (platform-specific logic)
                val installable = latestRelease?.assets?.filter { asset ->
                    installer.isAssetInstallable(asset.name)
                }.orEmpty()

                val primary = installer.choosePrimaryAsset(installable)

                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    repository = repo,
                    latestRelease = latestRelease,
                    stats = stats,
                    readmeMarkdown = readme,
                    installableAssets = installable,
                    primaryAsset = primary,
                    userProfile = userProfile,
                    systemArchitecture = installer.detectSystemArchitecture()
                )
            } catch (t: Throwable) {
                Logger.e { "Details load failed: ${t.message}" }
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load details"
                )
            }
        }
    }

    fun onAction(action: DetailsAction) {
        when (action) {
            DetailsAction.Retry -> {
                hasLoadedInitialData = false
                loadInitial()
            }

            DetailsAction.InstallPrimary -> {
                val primary = _state.value.primaryAsset
                val release = _state.value.latestRelease
                if (primary != null && release != null) {
                    installAsset(
                        downloadUrl = primary.downloadUrl,
                        assetName = primary.name,
                        sizeBytes = primary.size,
                        releaseTag = release.tagName
                    )
                }
            }

            is DetailsAction.DownloadAsset -> {
                val release = _state.value.latestRelease
                downloadAsset(
                    downloadUrl = action.downloadUrl,
                    assetName = action.assetName,
                    sizeBytes = action.sizeBytes,
                    releaseTag = release?.tagName ?: ""
                )
            }

            DetailsAction.CancelCurrentDownload -> {
                currentDownloadJob?.cancel()
                currentDownloadJob = null
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadProgressPercent = null
                )
            }

            DetailsAction.OpenRepoInBrowser -> {
                _state.value.repository?.htmlUrl?.let { openBrowser(it) }
            }

            DetailsAction.OpenAuthorInBrowser -> {
                _state.value.userProfile?.htmlUrl?.let { openBrowser(it) }
            }

            DetailsAction.OnNavigateBackClick -> { /* handled in UI host */
            }

            is DetailsAction.OpenAuthorInApp -> { /* handled in UI host */
            }
        }
    }

    private fun installAsset(
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String
    ) {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            try {
                appendLog(assetName, sizeBytes, releaseTag, "DownloadStarted")
                _state.value = _state.value.copy(
                    downloadError = null,
                    installError = null,
                    downloadProgressPercent = null
                )

                installer.ensurePermissionsOrThrow(
                    assetName.substringAfterLast('.', "").lowercase()
                )

                _state.value = _state.value.copy(downloadStage = DownloadStage.DOWNLOADING)

                downloader.download(downloadUrl, assetName).collect { p ->
                    _state.value = _state.value.copy(downloadProgressPercent = p.percent)
                    if (p.percent == 100) {
                        _state.value = _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                    }
                }

                val filePath = downloader.saveToFile(downloadUrl, assetName)
                appendLog(assetName, sizeBytes, releaseTag, "Downloaded")

                _state.value = _state.value.copy(downloadStage = DownloadStage.INSTALLING)
                val ext = assetName.substringAfterLast('.', "").lowercase()

                if (!installer.isSupported(ext)) {
                    throw IllegalStateException("Asset type .$ext not supported")
                }

                installer.install(filePath, ext)

                _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                appendLog(assetName, sizeBytes, releaseTag, "Installed")

            } catch (t: Throwable) {
                Logger.e { "Install failed: ${t.message}" }
                _state.value = _state.value.copy(
                    downloadStage = DownloadStage.IDLE,
                    installError = t.message
                )
                appendLog(assetName, sizeBytes, releaseTag, "Error: ${t.message}")
            }
        }
    }

    private fun downloadAsset(
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String
    ) {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            try {
                appendLog(assetName, sizeBytes, releaseTag, "DownloadStarted")
                _state.value = _state.value.copy(
                    isDownloading = true,
                    downloadError = null,
                    installError = null,
                    downloadProgressPercent = null
                )

                try {
                    downloader.download(downloadUrl, assetName).collect { p ->
                        _state.value = _state.value.copy(downloadProgressPercent = p.percent)
                    }
                } catch (_: Throwable) {
                }

                downloader.saveToFile(downloadUrl, assetName)
                _state.value = _state.value.copy(isDownloading = false)
                appendLog(assetName, sizeBytes, releaseTag, "Downloaded")

            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadError = t.message
                )
                appendLog(assetName, sizeBytes, releaseTag, "Error: ${t.message}")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun appendLog(assetName: String, size: Long, tag: String, result: String) {
        val now = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                day()
                char(' ')
                hour()
                char(':')
                minute()
                char(':')
                second()
            })
        val newItem = InstallLogItem(
            timeIso = now,
            assetName = assetName,
            assetSizeBytes = size,
            releaseTag = tag,
            result = result
        )
        _state.value = _state.value.copy(
            installLogs = listOf(newItem) + _state.value.installLogs
        )
    }

    override fun onCleared() {
        super.onCleared()
        currentDownloadJob?.cancel()
    }


}