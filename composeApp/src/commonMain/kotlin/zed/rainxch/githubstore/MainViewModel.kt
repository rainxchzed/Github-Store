package zed.rainxch.githubstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.data.data_source.TokenDataSource
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.repository.AuthenticationState
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.RateLimitRepository
import zed.rainxch.core.domain.repository.ThemesRepository
import zed.rainxch.core.domain.system.PackageMonitor

class MainViewModel(
    private val themesRepository: ThemesRepository,
    private val appStateManager: AppStateManager,
    private val packageMonitor: PackageMonitor,
    private val installedAppsRepository: InstalledAppsRepository,
    private val authenticationState: AuthenticationState,
    private val rateLimitRepository: RateLimitRepository,
    private val platform: Platform,
    private val logger: GitHubStoreLogger
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            authenticationState
                .isUserLoggedIn()
                .collect { isLoggedIn ->
                    _state.update { it.copy(isLoggedIn = isLoggedIn) }
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

        viewModelScope.launch {
            themesRepository
                .getFontTheme()
                .collect { fontTheme ->
                    _state.update {
                        it.copy(currentFontTheme = fontTheme)
                    }
                }
        }

        viewModelScope.launch {
            rateLimitRepository.rateLimitState.collect { rateLimitInfo ->
                _state.update { currentState ->
                    currentState.copy(
                        rateLimitInfo = rateLimitInfo,
                        showRateLimitDialog = rateLimitInfo?.isExhausted == true,
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
                        logger.d { "App ${app.packageName} no longer installed (not in system packages), removing from DB" }
                        installedAppsRepository.deleteInstalledApp(app.packageName)
                    } else if (app.installedVersionName == null) {  // Migrate only if new fields unset
                        if (platform.type == Platform.ANDROID) {
                            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
                            if (systemInfo != null) {
                                installedAppsRepository.updateApp(app.copy(
                                    installedVersionName = systemInfo.versionName,
                                    installedVersionCode = systemInfo.versionCode,
                                    latestVersionName = systemInfo.versionName,
                                    latestVersionCode = systemInfo.versionCode
                                ))
                                logger.d { "Migrated ${app.packageName}: set versionName/code from system" }
                            } else {
                                installedAppsRepository.updateApp(app.copy(
                                    installedVersionName = app.installedVersion,
                                    installedVersionCode = 0L,
                                    latestVersionName = app.installedVersion,
                                    latestVersionCode = 0L
                                ))
                                logger.d { "Migrated ${app.packageName}: fallback to tag as versionName" }
                            }
                        } else {
                            installedAppsRepository.updateApp(app.copy(
                                installedVersionName = app.installedVersion,
                                installedVersionCode = 0L,
                                latestVersionName = app.installedVersion,
                                latestVersionCode = 0L
                            ))
                            logger.d { "Migrated ${app.packageName} (desktop): fallback to tag as versionName" }
                        }
                    }
                }

                logger.d { "Robust system existence sync and data migration completed" }
            } catch (e: Exception) {
                logger.e { "Failed to sync existence or migrate data: ${e.message}" }
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