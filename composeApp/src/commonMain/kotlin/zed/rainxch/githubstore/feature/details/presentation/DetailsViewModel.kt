package zed.rainxch.githubstore.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.added_to_favourites
import githubstore.composeapp.generated.resources.app_installed_successfully
import githubstore.composeapp.generated.resources.app_updated_successfully
import githubstore.composeapp.generated.resources.auto_update_disabled
import githubstore.composeapp.generated.resources.auto_update_enabled
import githubstore.composeapp.generated.resources.installation_failed
import githubstore.composeapp.generated.resources.installer_saved_downloads
import githubstore.composeapp.generated.resources.removed_from_favourites
import githubstore.composeapp.generated.resources.shizuku_permission_error
import githubstore.composeapp.generated.resources.shizuku_permission_granted
import githubstore.composeapp.generated.resources.shizuku_permission_requested
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.data.local.db.entities.FavoriteRepo
import zed.rainxch.githubstore.core.data.local.db.entities.InstallSource
import zed.rainxch.githubstore.core.data.local.db.entities.InstalledApp
import zed.rainxch.githubstore.core.data.model.InstallationProgress
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.Installer
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.domain.Platform
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.core.domain.repository.FavouritesRepository
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.StarredRepository
import zed.rainxch.githubstore.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.details.presentation.DetailsEvent.OnMessage
import zed.rainxch.githubstore.feature.details.presentation.DetailsEvent.OnOpenRepositoryInApp
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.Cancelled
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.DownloadStarted
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.Downloaded
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.Error
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.Installed
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.OpenedInAppManager
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.PreparingForAppManager
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.UpdateStarted
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult.Updated
import java.io.File
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

class DetailsViewModel(
    private val repositoryId: Long,
    private val detailsRepository: DetailsRepository,
    private val downloader: Downloader,
    private val installer: Installer,
    private val platform: Platform,
    private val helper: BrowserHelper,
    private val installedAppsRepository: InstalledAppsRepository,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val packageMonitor: PackageMonitor,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var currentDownloadJob: Job? = null
    private var currentAssetName: String? = null

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

    private val _events = Channel<DetailsEvent>()
    val events = _events.receiveAsFlow()

    @OptIn(ExperimentalTime::class)
    private fun loadInitial() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val syncResult = syncInstalledAppsUseCase()
                if (syncResult.isFailure) {
                    Logger.w { "Sync had issues but continuing: ${syncResult.exceptionOrNull()?.message}" }
                }

                val repo = detailsRepository.getRepositoryById(repositoryId)
                val isFavoriteDeferred = async {
                    try {
                        favouritesRepository.isFavoriteSync(repo.id)
                    } catch (t: Throwable) {
                        Logger.e { "Failed to load if repo is favourite: ${t.localizedMessage}" }
                        false
                    }
                }
                val isFavorite = isFavoriteDeferred.await()
                val isStarredDeferred = async {
                    try {
                        starredRepository.isStarred(repo.id)
                    } catch (t: Throwable) {
                        Logger.e { "Failed to load if repo is starred: ${t.localizedMessage}" }
                        false
                    }
                }
                val isStarred = isStarredDeferred.await()

                val owner = repo.owner.login
                val name = repo.name

                _state.value = _state.value.copy(
                    repository = repo,
                    isFavourite = isFavorite,
                    isStarred = isStarred,
                )

                val latestReleaseDeferred = async {
                    try {
                        detailsRepository.getLatestPublishedRelease(
                            owner = owner,
                            repo = name,
                            defaultBranch = repo.defaultBranch
                        )
                    } catch (t: Throwable) {
                        Logger.w { "Failed to load latest release: ${t.message}" }
                        null
                    }
                }

                val statsDeferred = async {
                    try {
                        detailsRepository.getRepoStats(owner, name)
                    } catch (_: Throwable) {
                        null
                    }
                }

                val readmeDeferred = async {
                    try {
                        detailsRepository.getReadme(
                            owner = owner,
                            repo = name,
                            defaultBranch = repo.defaultBranch
                        )
                    } catch (_: Throwable) {
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

                val installedAppDeferred = async {
                    try {
                        val dbApp = installedAppsRepository.getAppByRepoId(repo.id)

                        if (dbApp != null) {
                            if (dbApp.isPendingInstall &&
                                packageMonitor.isPackageInstalled(dbApp.packageName)
                            ) {
                                installedAppsRepository.updatePendingStatus(
                                    dbApp.packageName,
                                    false
                                )
                                installedAppsRepository.getAppByPackage(dbApp.packageName)
                            } else {
                                dbApp
                            }
                        } else {
                            null
                        }
                    } catch (t: Throwable) {
                        Logger.e { "Failed to load installed app: ${t.message}" }
                        null
                    }
                }

                val isObtainiumEnabled = platform.type == PlatformType.ANDROID
                val isAppManagerEnabled = platform.type == PlatformType.ANDROID

                val isShizukuEnabled = platform.type == PlatformType.ANDROID
                val isShizukuInstalled = if (isShizukuEnabled) {
                    installer.isShizukuInstalled()
                } else false

                val isShizukuRunning = if (isShizukuInstalled) {
                    installer.isShizukuAvailable()
                } else false

                val hasShizukuPermission = if (isShizukuRunning) {
                    try {
                        installer.isShizukuAvailable()
                    } catch (e: Exception) {
                        Logger.e(e) { "Error checking Shizuku permission" }
                        false
                    }
                } else false

                val latestRelease = latestReleaseDeferred.await()
                val stats = statsDeferred.await()
                val readme = readmeDeferred.await()
                val userProfile = userProfileDeferred.await()
                val installedApp = installedAppDeferred.await()

                val installable = latestRelease?.assets?.filter { asset ->
                    installer.isAssetInstallable(asset.name)
                }.orEmpty()

                val primary = installer.choosePrimaryAsset(installable)

                val isObtainiumAvailable = installer.isObtainiumInstalled()
                val isAppManagerAvailable = installer.isAppManagerInstalled()

                Logger.d { "Loaded repo: ${repo.name}, installedApp: ${installedApp?.packageName}" }
                Logger.d { "Shizuku status: enabled=$isShizukuEnabled, installed=$isShizukuInstalled, running=$isShizukuRunning, permission=$hasShizukuPermission" }

                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    repository = repo,
                    latestRelease = latestRelease,
                    stats = stats,
                    readmeMarkdown = readme?.first,
                    readmeLanguage = readme?.second,
                    installableAssets = installable,
                    primaryAsset = primary,
                    userProfile = userProfile,
                    systemArchitecture = installer.detectSystemArchitecture(),
                    isObtainiumAvailable = isObtainiumAvailable,
                    isObtainiumEnabled = isObtainiumEnabled,
                    isAppManagerAvailable = isAppManagerAvailable,
                    isAppManagerEnabled = isAppManagerEnabled,
                    installedApp = installedApp,
                    isShizukuEnabled = isShizukuEnabled,
                    isShizukuInstalled = isShizukuInstalled,
                    isShizukuRunning = isShizukuRunning,
                    hasShizukuPermission = hasShizukuPermission,
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

    @OptIn(ExperimentalTime::class)
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

                val assetName = currentAssetName
                if (assetName != null) {
                    viewModelScope.launch {
                        try {
                            val deleted = downloader.cancelDownload(assetName)
                            Logger.d { "Cancel download - file deleted: $deleted" }

                            appendLog(
                                assetName = assetName,
                                size = 0L,
                                tag = _state.value.latestRelease?.tagName ?: "",
                                result = Cancelled
                            )
                        } catch (t: Throwable) {
                            Logger.e { "Failed to cancel download: ${t.message}" }
                        }
                    }
                }

                currentAssetName = null
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadProgressPercent = null,
                    downloadStage = DownloadStage.IDLE
                )
            }

            DetailsAction.OnToggleFavorite -> {
                viewModelScope.launch {
                    try {
                        val repo = _state.value.repository ?: return@launch
                        val latestRelease = _state.value.latestRelease

                        val favoriteRepo = FavoriteRepo(
                            repoId = repo.id,
                            repoName = repo.name,
                            repoOwner = repo.owner.login,
                            repoOwnerAvatarUrl = repo.owner.avatarUrl,
                            repoDescription = repo.description,
                            primaryLanguage = repo.language,
                            repoUrl = repo.htmlUrl,
                            latestVersion = latestRelease?.tagName,
                            latestReleaseUrl = latestRelease?.htmlUrl,
                            addedAt = System.now().toEpochMilliseconds(),
                            lastSyncedAt = System.now().toEpochMilliseconds()
                        )

                        favouritesRepository.toggleFavorite(favoriteRepo)

                        val newFavoriteState = favouritesRepository.isFavoriteSync(repo.id)
                        _state.value = _state.value.copy(isFavourite = newFavoriteState)

                        _events.send(
                            element = OnMessage(
                                message = getString(
                                    resource = if (newFavoriteState) {
                                        Res.string.added_to_favourites
                                    } else {
                                        Res.string.removed_from_favourites
                                    }
                                )
                            )
                        )

                    } catch (t: Throwable) {
                        Logger.e { "Failed to toggle favorite: ${t.message}" }
                    }
                }
            }

            DetailsAction.CheckForUpdates -> {
                viewModelScope.launch {
                    try {
                        syncInstalledAppsUseCase()

                        val installedApp = _state.value.installedApp ?: return@launch
                        val hasUpdate =
                            installedAppsRepository.checkForUpdates(installedApp.packageName)

                        if (hasUpdate) {
                            val updatedApp =
                                installedAppsRepository.getAppByPackage(installedApp.packageName)
                            _state.value = _state.value.copy(installedApp = updatedApp)
                        }
                    } catch (t: Throwable) {
                        Logger.e { "Failed to check for updates: ${t.message}" }
                    }
                }
            }

            DetailsAction.UpdateApp -> {
                val installedApp = _state.value.installedApp
                val latestRelease = _state.value.latestRelease

                if (installedApp != null && latestRelease != null && installedApp.isUpdateAvailable) {
                    val latestAsset = _state.value.installableAssets.firstOrNull {
                        it.name == installedApp.latestAssetName
                    } ?: _state.value.primaryAsset

                    if (latestAsset != null) {
                        installAsset(
                            downloadUrl = latestAsset.downloadUrl,
                            assetName = latestAsset.name,
                            sizeBytes = latestAsset.size,
                            releaseTag = latestRelease.tagName,
                            isUpdate = true
                        )
                    }
                }
            }

            DetailsAction.OpenRepoInBrowser -> {
                _state.value.repository?.htmlUrl?.let {
                    helper.openUrl(url = it)
                }
            }

            DetailsAction.OpenAuthorInBrowser -> {
                _state.value.userProfile?.htmlUrl?.let {
                    helper.openUrl(url = it)
                }
            }

            DetailsAction.OpenInObtainium -> {
                val repo = _state.value.repository
                repo?.owner?.login?.let {
                    installer.openInObtainium(
                        repoOwner = it,
                        repoName = repo.name,
                        onOpenInstaller = {
                            viewModelScope.launch {
                                _events.send(
                                    OnOpenRepositoryInApp(OBTAINIUM_REPO_ID)
                                )
                            }
                        }
                    )
                }
                _state.update {
                    it.copy(isInstallDropdownExpanded = false)
                }
            }

            DetailsAction.OpenInAppManager -> {
                viewModelScope.launch {
                    try {
                        val primary = _state.value.primaryAsset
                        val release = _state.value.latestRelease

                        if (primary != null && release != null) {
                            currentAssetName = primary.name

                            appendLog(
                                assetName = primary.name,
                                size = primary.size,
                                tag = release.tagName,
                                result = PreparingForAppManager
                            )

                            _state.value = _state.value.copy(
                                downloadError = null,
                                installError = null,
                                downloadProgressPercent = null,
                                downloadStage = DownloadStage.DOWNLOADING
                            )

                            downloader.download(primary.downloadUrl, primary.name).collect { p ->
                                _state.value =
                                    _state.value.copy(downloadProgressPercent = p.percent)
                                if (p.percent == 100) {
                                    _state.value =
                                        _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                                }
                            }

                            val filePath = downloader.getDownloadedFilePath(primary.name)
                                ?: throw IllegalStateException("Downloaded file not found")

                            appendLog(
                                assetName = primary.name,
                                size = primary.size,
                                tag = release.tagName,
                                result = Downloaded
                            )

                            _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                            currentAssetName = null

                            installer.openInAppManager(
                                filePath = filePath,
                                onOpenInstaller = {
                                    viewModelScope.launch {
                                        _events.send(
                                            OnOpenRepositoryInApp(APP_MANAGER_REPO_ID)
                                        )
                                    }
                                }
                            )

                            appendLog(
                                assetName = primary.name,
                                size = primary.size,
                                tag = release.tagName,
                                result = OpenedInAppManager
                            )
                        }
                    } catch (t: Throwable) {
                        Logger.e { "Failed to open in AppManager: ${t.message}" }
                        _state.value = _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            installError = t.message
                        )
                        currentAssetName = null

                        _state.value.primaryAsset?.let { asset ->
                            _state.value.latestRelease?.let { release ->
                                appendLog(
                                    assetName = asset.name,
                                    size = asset.size,
                                    tag = release.tagName,
                                    result = Error(t.message)
                                )
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(isInstallDropdownExpanded = false)
                }
            }

            DetailsAction.OnToggleInstallDropdown -> {
                _state.update {
                    it.copy(isInstallDropdownExpanded = !it.isInstallDropdownExpanded)
                }
            }

            DetailsAction.RequestShizukuPermission -> {
                viewModelScope.launch {
                    try {
                        val alreadyGranted = installer.requestShizukuPermission()

                        if (alreadyGranted) {
                            _state.update {
                                it.copy(
                                    hasShizukuPermission = true,
                                    isShizukuRunning = true
                                )
                            }
                            _events.send(
                                OnMessage(getString(Res.string.shizuku_permission_granted))
                            )
                        } else {
                            _events.send(
                                OnMessage(getString(Res.string.shizuku_permission_requested))
                            )
                            delay(500)
                            refreshShizukuStatusInternal()
                        }
                    } catch (e: Exception) {
                        Logger.e(e) { "Error requesting Shizuku permission" }
                        _events.send(
                            OnMessage(getString(Res.string.shizuku_permission_error))
                        )
                    }
                }
            }

            DetailsAction.OnNavigateBackClick -> {
            }

            is DetailsAction.OpenDeveloperProfile -> {
            }

            is DetailsAction.OnMessage -> {
            }

            DetailsAction.OpenShizukuSetupDialog -> {
                _state.update { it.copy(showShizukuSetupDialog = true) }
            }

            DetailsAction.CloseShizukuSetupDialog -> {
                _state.update { it.copy(showShizukuSetupDialog = false) }
            }

            DetailsAction.OnShizukuRequestPermission -> {
                viewModelScope.launch {
                    try {
                        val granted = installer.requestShizukuPermission()

                        if (granted) {
                            _state.update {
                                it.copy(
                                    hasShizukuPermission = true,
                                    isShizukuRunning = true
                                )
                            }
                        } else {
                            delay(1000)
                            refreshShizukuStatusInternal()
                        }
                    } catch (e: Exception) {
                        Logger.e(e) { "Error in OnShizukuRequestPermission" }
                    }
                }
            }

            DetailsAction.RefreshShizukuStatus -> {
                refreshShizukuStatusInternal()
            }

            DetailsAction.OpenShizukuApp -> {
                try {
                    installer.openShizukuApp()

                    viewModelScope.launch {
                        delay(500)
                        refreshShizukuStatusInternal()
                    }
                } catch (e: Exception) {
                    Logger.e(e) { "Error opening Shizuku app" }
                }
            }
        }
    }

    private fun refreshShizukuStatusInternal() {
        viewModelScope.launch {
            try {
                val isShizukuInstalled = installer.isShizukuInstalled()
                val isShizukuRunning = if (isShizukuInstalled) {
                    installer.isShizukuAvailable()
                } else false

                val hasShizukuPermission = if (isShizukuRunning) {
                    installer.isShizukuAvailable()
                } else false

                _state.update {
                    it.copy(
                        isShizukuInstalled = isShizukuInstalled,
                        isShizukuRunning = isShizukuRunning,
                        hasShizukuPermission = hasShizukuPermission
                    )
                }

                Logger.d { "Shizuku status refreshed: installed=$isShizukuInstalled, running=$isShizukuRunning, permission=$hasShizukuPermission, available=${_state.value.isShizukuAvailable}" }
            } catch (e: Exception) {
                Logger.e(e) { "Error refreshing Shizuku status" }
            }
        }
    }

    private fun installAsset(
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String,
        isUpdate: Boolean = false
    ) {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            try {
                currentAssetName = assetName

                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = if (isUpdate) UpdateStarted else DownloadStarted
                )

                _state.value = _state.value.copy(
                    downloadError = null,
                    installError = null,
                    downloadProgressPercent = null,
                    installProgressPercent = null
                )

                val useShizuku = _state.value.isShizukuAvailable

                Logger.d { "Installing with Shizuku: $useShizuku" }

                if (!useShizuku) {
                    installer.ensurePermissionsOrThrow(
                        extOrMime = assetName.substringAfterLast('.', "").lowercase()
                    )
                }

                _state.value = _state.value.copy(downloadStage = DownloadStage.DOWNLOADING)
                downloader.download(downloadUrl, assetName).collect { p ->
                    _state.value = _state.value.copy(downloadProgressPercent = p.percent)
                    if (p.percent == 100) {
                        _state.value = _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                    }
                }

                val filePath = downloader.getDownloadedFilePath(assetName)
                    ?: throw IllegalStateException("Downloaded file not found")

                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = Downloaded
                )

                if (useShizuku) {
                    _state.value = _state.value.copy(downloadStage = DownloadStage.INSTALLING)

                    installer.installWithShizukuProgress(File(filePath)).collect { progress ->
                        when (progress) {
                            is InstallationProgress.Preparing -> {
                                _state.value = _state.value.copy(
                                    downloadStage = DownloadStage.INSTALLING,
                                    installProgressPercent = 0
                                )
                            }

                            is InstallationProgress.CreatingSession -> {
                                _state.value = _state.value.copy(installProgressPercent = 10)
                            }

                            is InstallationProgress.Installing -> {
                                _state.value = _state.value.copy(
                                    installProgressPercent = 10 + (progress.progress * 0.8).toInt()
                                )
                            }

                            is InstallationProgress.Finalizing -> {
                                _state.value = _state.value.copy(installProgressPercent = 95)
                            }

                            is InstallationProgress.Success -> {
                                _state.value = _state.value.copy(installProgressPercent = 100)

                                if (platform.type == PlatformType.ANDROID) {
                                    saveInstalledAppToDatabase(
                                        assetName = assetName,
                                        assetUrl = downloadUrl,
                                        assetSize = sizeBytes,
                                        releaseTag = releaseTag,
                                        isUpdate = isUpdate,
                                        filePath = filePath
                                    )
                                }

                                appendLog(
                                    assetName = assetName,
                                    size = sizeBytes,
                                    tag = releaseTag,
                                    result = if (isUpdate) Updated else Installed
                                )

                                _events.send(
                                    OnMessage(
                                        getString(
                                            if (isUpdate) Res.string.app_updated_successfully
                                            else Res.string.app_installed_successfully
                                        )
                                    )
                                )
                            }

                            is InstallationProgress.Error -> {
                                throw IllegalStateException(progress.message)
                            }
                        }
                    }
                } else {
                    _state.value = _state.value.copy(downloadStage = DownloadStage.INSTALLING)
                    val ext = assetName.substringAfterLast('.', "").lowercase()

                    if (!installer.isSupported(ext)) {
                        throw IllegalStateException("Asset type .$ext not supported")
                    }

                    if (platform.type == PlatformType.ANDROID) {
                        saveInstalledAppToDatabase(
                            assetName = assetName,
                            assetUrl = downloadUrl,
                            assetSize = sizeBytes,
                            releaseTag = releaseTag,
                            isUpdate = isUpdate,
                            filePath = filePath
                        )
                    } else {
                        viewModelScope.launch {
                            _events.send(OnMessage(getString(Res.string.installer_saved_downloads)))
                        }
                    }

                    installer.install(filePath, ext)

                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = if (isUpdate) Updated else Installed
                    )
                }

                _state.value = _state.value.copy(
                    downloadStage = DownloadStage.IDLE,
                    installProgressPercent = null
                )
                currentAssetName = null

            } catch (t: Throwable) {
                Logger.e { "Install failed: ${t.message}" }
                t.printStackTrace()
                _state.value = _state.value.copy(
                    downloadStage = DownloadStage.IDLE,
                    installError = t.message,
                    installProgressPercent = null
                )
                currentAssetName = null
                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = Error(t.message)
                )

                viewModelScope.launch {
                    _events.send(
                        OnMessage(
                            getString(Res.string.installation_failed) + ": ${t.message}"
                        )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun saveInstalledAppToDatabase(
        assetName: String,
        assetUrl: String,
        assetSize: Long,
        releaseTag: String,
        isUpdate: Boolean,
        filePath: String
    ) {
        try {
            val repo = _state.value.repository ?: return

            var packageName: String
            var appName = repo.name
            var versionName: String? = null
            var versionCode = 0L

            if (platform.type == PlatformType.ANDROID && assetName.lowercase().endsWith(".apk")) {
                val apkInfo = installer.getApkInfoExtractor().extractPackageInfo(filePath)
                if (apkInfo != null) {
                    packageName = apkInfo.packageName
                    appName = apkInfo.appName
                    versionName = apkInfo.versionName
                    versionCode = apkInfo.versionCode
                    Logger.d { "Extracted APK info - package: $packageName, name: $appName, versionName: $versionName, versionCode: $versionCode" }
                } else {
                    Logger.e { "Failed to extract APK info for $assetName" }
                    return
                }
            } else {
                packageName = "app.github.${repo.owner.login}.${repo.name}".lowercase()
            }

            if (isUpdate) {
                installedAppsRepository.updateAppVersion(
                    packageName = packageName,
                    newTag = releaseTag,
                    newAssetName = assetName,
                    newAssetUrl = assetUrl,
                    newVersionName = versionName ?: "unknown",
                    newVersionCode = versionCode
                )
            } else {
                val installedApp = InstalledApp(
                    packageName = packageName,
                    repoId = repo.id,
                    repoName = repo.name,
                    repoOwner = repo.owner.login,
                    repoOwnerAvatarUrl = repo.owner.avatarUrl,
                    repoDescription = repo.description,
                    primaryLanguage = repo.language,
                    repoUrl = repo.htmlUrl,
                    installedVersion = releaseTag,
                    installedAssetName = assetName,
                    installedAssetUrl = assetUrl,
                    latestVersion = releaseTag,
                    latestAssetName = assetName,
                    latestAssetUrl = assetUrl,
                    latestAssetSize = assetSize,
                    appName = appName,
                    installSource = InstallSource.THIS_APP,
                    installedAt = System.now().toEpochMilliseconds(),
                    lastCheckedAt = System.now().toEpochMilliseconds(),
                    lastUpdatedAt = System.now().toEpochMilliseconds(),
                    isUpdateAvailable = false,
                    updateCheckEnabled = true,
                    releaseNotes = "",
                    systemArchitecture = installer.detectSystemArchitecture().name,
                    fileExtension = assetName.substringAfterLast('.', ""),
                    isPendingInstall = true,
                    installedVersionName = versionName,
                    installedVersionCode = versionCode,
                    latestVersionName = versionName,
                    latestVersionCode = versionCode
                )

                installedAppsRepository.saveInstalledApp(installedApp)
            }

            if (_state.value.isFavourite) {
                favouritesRepository.updateFavoriteInstallStatus(
                    repoId = repo.id,
                    installed = true,
                    packageName = packageName
                )
            }

            delay(500)
            val updatedApp = installedAppsRepository.getAppByPackage(packageName)
            _state.value = _state.value.copy(installedApp = updatedApp)

            Logger.d { "Successfully saved and reloaded app: ${updatedApp?.packageName}" }

        } catch (t: Throwable) {
            Logger.e { "Failed to save installed app to database: ${t.message}" }
            t.printStackTrace()
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
                currentAssetName = assetName

                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = DownloadStarted
                )
                _state.value = _state.value.copy(
                    isDownloading = true,
                    downloadError = null,
                    installError = null,
                    downloadProgressPercent = null
                )

                downloader.download(downloadUrl, assetName).collect { p ->
                    _state.value = _state.value.copy(downloadProgressPercent = p.percent)
                }

                _state.value = _state.value.copy(isDownloading = false)
                currentAssetName = null
                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = Downloaded
                )

            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadError = t.message
                )
                currentAssetName = null
                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = Error(t.message)
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun appendLog(
        assetName: String,
        size: Long,
        tag: String,
        result: LogResult
    ) {
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

        currentAssetName?.let { assetName ->
            viewModelScope.launch {
                downloader.cancelDownload(assetName)
            }
        }
    }

    private companion object {
        const val OBTAINIUM_REPO_ID: Long = 523534328
        const val APP_MANAGER_REPO_ID: Long = 268006778
    }
}