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
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.ThemesRepository

class MainViewModel(
    private val tokenDataSource: TokenDataSource,
    private val themesRepository: ThemesRepository,
    private val appStateManager: AppStateManager,
    private val packageMonitor: PackageMonitor,
    private val installedAppsRepository: InstalledAppsRepository,
    private val platform: Platform
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val initialToken = tokenDataSource.reloadFromStore()
            _state.update {
                it.copy(
                    isCheckingAuth = false,
                    isLoggedIn = initialToken != null
                )
            }
            Logger.d("MainViewModel") { "Initial token loaded: ${initialToken != null}" }
        }

        viewModelScope.launch(Dispatchers.IO) {
            tokenDataSource
                .tokenFlow
                .drop(1)
                .distinctUntilChanged()
                .collect { authInfo ->
                    _state.update { it.copy(isLoggedIn = authInfo != null) }
                }
        }

        viewModelScope.launch {
            themesRepository
                .getThemeColor()
                .collect { theme ->
                    _state.update {
                        it.copy(currentColorTheme = theme)
                    }
                }
        }
        viewModelScope.launch {
            themesRepository
                .getAmoledTheme()
                .collect { isAmoled ->
                    _state.update {
                        it.copy(isAmoledTheme = isAmoled)
                    }
                }
        }
        viewModelScope.launch {
            themesRepository
                .getIsDarkTheme()
                .collect { isDarkTheme ->
                    _state.update {
                        it.copy(isDarkTheme = isDarkTheme)
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            appStateManager.appState.collect { appState ->
                _state.update {
                    it.copy(
                        rateLimitInfo = appState.rateLimitInfo,
                        showRateLimitDialog = appState.showRateLimitDialog
                    )
                }
            }
        }

        viewModelScope.launch {
            themesRepository
                .getFontTheme()
                .collect { fontTheme ->
                    _state.update {
                        it.copy(currentFontTheme = fontTheme)
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val installedPackageNames = packageMonitor.getAllInstalledPackageNames()

                val appsInDb = installedAppsRepository.getAllInstalledApps().first()

                appsInDb.forEach { app ->
                    if (!installedPackageNames.contains(app.packageName)) {
                        Logger.d { "App ${app.packageName} no longer installed (not in system packages), removing from DB" }
                        installedAppsRepository.deleteInstalledApp(app.packageName)
                    } else if (app.installedVersionName == null) {  // Migrate only if new fields unset
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
                                Logger.d { "Migrated ${app.packageName}: fallback to tag as versionName" }
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
                            Logger.d { "Migrated ${app.packageName} (desktop): fallback to tag as versionName" }
                        }
                    }
                }

                Logger.d { "Robust system existence sync and data migration completed" }
            } catch (e: Exception) {
                Logger.e { "Failed to sync existence or migrate data: ${e.message}" }
            }

            installedAppsRepository.checkAllForUpdates()
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.DismissRateLimitDialog -> {
                appStateManager.dismissRateLimitDialog()
            }
        }
    }
}