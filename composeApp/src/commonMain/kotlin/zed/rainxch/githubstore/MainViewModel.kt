package zed.rainxch.githubstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.Platform
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.ThemesRepository

class MainViewModel(
    private val githubTokenDataSource: TokenDataSource,
    private val gitlabTokenDataSource: TokenDataSource,
    private val themesRepository: ThemesRepository,
    private val appStateManager: AppStateManager,
    private val packageMonitor: PackageMonitor,
    private val installedAppsRepository: InstalledAppsRepository,
    private val platform: Platform,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val githubToken = githubTokenDataSource.reloadFromStore(ApiPlatform.Github)
            val gitlabToken = gitlabTokenDataSource.reloadFromStore(ApiPlatform.GitLab)

            _state.update {
                it.copy(
                    isCheckingAuth = false,
                    isGithubLoggedIn = githubToken != null,
                    isGitlabLoggedIn = gitlabToken != null
                )
            }
            Logger.d("MainViewModel") {
                "Initial tokens loaded - GitHub: ${githubToken != null}, GitLab: ${gitlabToken != null}"
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            githubTokenDataSource
                .tokenFlow(ApiPlatform.Github)
                .drop(1)
                .distinctUntilChanged()
                .collect { authInfo ->
                    _state.update { it.copy(isGithubLoggedIn = authInfo != null) }
                    Logger.d("MainViewModel") { "GitHub auth changed: ${authInfo != null}" }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            gitlabTokenDataSource
                .tokenFlow(ApiPlatform.GitLab)
                .drop(1)
                .distinctUntilChanged()
                .collect { authInfo ->
                    _state.update { it.copy(isGitlabLoggedIn = authInfo != null) }
                    Logger.d("MainViewModel") { "GitLab auth changed: ${authInfo != null}" }
                }
        }

        viewModelScope.launch {
            themesRepository
                .getThemeColor()
                .collect { theme ->
                    _state.update { it.copy(currentColorTheme = theme) }
                }
        }

        viewModelScope.launch {
            themesRepository
                .getAmoledTheme()
                .collect { isAmoled ->
                    _state.update { it.copy(isAmoledTheme = isAmoled) }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            appStateManager.appState.collect { appState ->
                _state.update {
                    it.copy(
                        githubRateLimitInfo = appState.githubRateLimitInfo,
                        gitlabRateLimitInfo = appState.gitlabRateLimitInfo,
                        showRateLimitDialog = appState.showRateLimitDialog,
                        rateLimitDialogPlatform = appState.rateLimitDialogPlatform
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
                                Logger.d { "Migrated ${app.packageName}: set version from system" }
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

                Logger.d { "Sync and migration completed" }
            } catch (e: Exception) {
                Logger.e { "Failed to sync/migrate: ${e.message}" }
            }

            installedAppsRepository.checkAllForUpdates()
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.DismissRateLimitDialog -> {
                appStateManager.dismissRateLimitDialog()
            }

            is MainAction.SwitchPlatform -> {
                _state.update { it.copy(currentApiPlatform = action.platform) }

                if (action.platform == ApiPlatform.GitLab) {
                    if (!_state.value.isGitlabLoggedIn) {
                        appStateManager.dismissRateLimitDialog()
                        appStateManager.triggerAuthDialog(ApiPlatform.GitLab)
                    }
                }
            }
        }
    }
}