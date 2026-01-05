package zed.rainxch.githubstore.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.added_to_favourites
import githubstore.composeapp.generated.resources.installer_saved_downloads
import githubstore.composeapp.generated.resources.removed_from_favourites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.data.local.db.entities.FavoriteRepo
import zed.rainxch.githubstore.core.data.local.db.entities.InstallSource
import zed.rainxch.githubstore.core.data.local.db.entities.InstalledApp
import zed.rainxch.githubstore.core.domain.Platform
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.core.domain.repository.FavoritesRepository
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.Installer
import zed.rainxch.githubstore.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.details.presentation.model.LogResult
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

class DetailsViewModel(
    private val repositoryId: Int,
    private val detailsRepository: DetailsRepository,
    private val downloader: Downloader,
    private val installer: Installer,
    private val platform: Platform,
    private val helper: BrowserHelper,
    private val installedAppsRepository: InstalledAppsRepository,
    private val favoritesRepository: FavoritesRepository,
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

                val repo = detailsRepository.getRepositoryById(repositoryId.toLong())
                val isFavoriteDeferred = async {
                    try {
                        favoritesRepository.isFavoriteSync(repo.id)
                    } catch (t: Throwable) {
                        false
                    }
                }
                val isFavorite = isFavoriteDeferred.await()

                val owner = repo.owner.login
                val name = repo.name

                _state.value = _state.value.copy(repository = repo, isFavorite = isFavorite)

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
                    } catch (t: Throwable) {
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

    private suspend fun syncSystemExistenceAndMigrate() {
        withContext(Dispatchers.IO) {
            try {
                val installedPackageNames = packageMonitor.getAllInstalledPackageNames()
                val appsInDb = installedAppsRepository.getAllInstalledApps().first()

                appsInDb.forEach { app ->
                    if (!installedPackageNames.contains(app.packageName)) {
                        Logger.d { "App ${app.packageName} no longer installed, removing from DB" }
                        installedAppsRepository.deleteInstalledApp(app.packageName)
                    } else if (app.installedVersionName == null) {
                        if (platform.type == PlatformType.ANDROID) {
                            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
                            if (systemInfo != null) {
                                installedAppsRepository.updateApp(
                                    app.copy(
                                        installedVersionName = systemInfo.versionName,
                                        installedVersionCode = systemInfo.versionCode,
                                        latestVersionName = systemInfo.versionName,
                                        latestVersionCode = systemInfo.versionCode
                                    )
                                )
                                Logger.d { "Migrated ${app.packageName}: set versionName/code from system" }
                            } else {
                                installedAppsRepository.updateApp(
                                    app.copy(
                                        installedVersionName = app.installedVersion,
                                        installedVersionCode = 0L,
                                        latestVersionName = app.installedVersion,
                                        latestVersionCode = 0L
                                    )
                                )
                                Logger.d { "Migrated ${app.packageName}: fallback to tag" }
                            }
                        } else {
                            installedAppsRepository.updateApp(
                                app.copy(
                                    installedVersionName = app.installedVersion,
                                    installedVersionCode = 0L,
                                    latestVersionName = app.installedVersion,
                                    latestVersionCode = 0L
                                )
                            )
                            Logger.d { "Migrated ${app.packageName} (desktop): fallback to tag" }
                        }
                    }
                }

                Logger.d { "System existence sync and data migration completed" }
            } catch (e: Exception) {
                Logger.e { "Failed to sync existence or migrate data: ${e.message}" }
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
                                result = LogResult.Cancelled
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

                        favoritesRepository.toggleFavorite(favoriteRepo)

                        val newFavoriteState = favoritesRepository.isFavoriteSync(repo.id)
                        _state.value = _state.value.copy(isFavorite = newFavoriteState)

                        _events.send(
                            element = DetailsEvent.OnMessage(
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
                                    DetailsEvent.OnOpenRepositoryInApp(OBTAINIUM_REPO_ID)
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
                                result = LogResult.PreparingForAppManager
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
                                result = LogResult.Downloaded
                            )

                            _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                            currentAssetName = null

                            installer.openInAppManager(
                                filePath = filePath,
                                onOpenInstaller = {
                                    viewModelScope.launch {
                                        _events.send(
                                            DetailsEvent.OnOpenRepositoryInApp(APP_MANAGER_REPO_ID)
                                        )
                                    }
                                }
                            )

                            appendLog(
                                assetName = primary.name,
                                size = primary.size,
                                tag = release.tagName,
                                result = LogResult.OpenedInAppManager
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
                                    result = LogResult.Error(t.message)
                                )
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(isInstallDropdownExpanded = false)
                }
            }

            DetailsAction.OnNavigateBackClick -> { /* handled in UI host */
            }

            is DetailsAction.OpenAuthorInApp -> { /* handled in UI host */
            }

            DetailsAction.OnToggleInstallDropdown -> {
                _state.update {
                    it.copy(isInstallDropdownExpanded = !it.isInstallDropdownExpanded)
                }
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
                    result = if (isUpdate) {
                        LogResult.UpdateStarted
                    } else LogResult.DownloadStarted
                )
                _state.value = _state.value.copy(
                    downloadError = null,
                    installError = null,
                    downloadProgressPercent = null
                )

                installer.ensurePermissionsOrThrow(
                    extOrMime = assetName.substringAfterLast('.', "").lowercase()
                )

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
                    result = LogResult.Downloaded
                )

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
                        _events.send(DetailsEvent.OnMessage(getString(Res.string.installer_saved_downloads)))
                    }
                }

                installer.install(filePath, ext)

                _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                currentAssetName = null
                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = if (isUpdate) {
                        LogResult.Updated
                    } else LogResult.Installed
                )

            } catch (t: Throwable) {
                Logger.e { "Install failed: ${t.message}" }
                t.printStackTrace()
                _state.value = _state.value.copy(
                    downloadStage = DownloadStage.IDLE,
                    installError = t.message
                )
                currentAssetName = null
                appendLog(
                    assetName = assetName,
                    size = sizeBytes,
                    tag = releaseTag,
                    result = LogResult.Error(t.message)
                )
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
            var versionCode: Long = 0L

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

            if (_state.value.isFavorite) {
                favoritesRepository.updateFavoriteInstallStatus(
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
                    result = LogResult.DownloadStarted
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
                    result = LogResult.Downloaded
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
                    result = LogResult.Error(t.message)
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

    private fun normalizeVersion(version: String): String {
        return version.removePrefix("v").removePrefix("V").trim()
    }

    private companion object {
        const val OBTAINIUM_REPO_ID = 523534328
        const val APP_MANAGER_REPO_ID = 268006778
    }
}