package zed.rainxch.githubstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.ThemesRepository
import zed.rainxch.githubstore.core.domain.use_cases.MigrateInstalledAppsDataUseCase
import zed.rainxch.githubstore.core.presentation.model.AppTheme
import zed.rainxch.githubstore.core.presentation.model.FontTheme

/**
 * Main ViewModel for the application.
 * 
 * Manages authentication state, theme preferences, rate limit information,
 * and initial data migration.
 */
class MainViewModel(
    private val tokenDataSource: TokenDataSource,
    private val themesRepository: ThemesRepository,
    private val appStateManager: AppStateManager,
    private val migrateInstalledAppsDataUseCase: MigrateInstalledAppsDataUseCase,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        initializeAuthState()
        observeAuthChanges()
        observeThemePreferences()
        observeAppState()
        performInitialMigration()
    }

    /**
     * Loads initial authentication state from storage.
     */
    private fun initializeAuthState() {
        viewModelScope.launch(Dispatchers.IO) {
            val initialToken = tokenDataSource.reloadFromStore()
            _state.update {
                it.copy(
                    isCheckingAuth = false,
                    isLoggedIn = initialToken != null
                )
            }
        }
    }

    /**
     * Observes authentication changes (login/logout).
     */
    private fun observeAuthChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            tokenDataSource
                .tokenFlow
                .drop(1) // Skip initial value
                .distinctUntilChanged()
                .collect { authInfo ->
                    _state.update { it.copy(isLoggedIn = authInfo != null) }
                }
        }
    }

    /**
     * Observes all theme preferences with a single combined flow.
     */
    private fun observeThemePreferences() {
        viewModelScope.launch {
            combine(
                themesRepository.getThemeColor(),
                themesRepository.getAmoledTheme(),
                themesRepository.getIsDarkTheme(),
                themesRepository.getFontTheme()
            ) { color, amoled, dark, font ->
                ThemePreferences(color, amoled, dark, font)
            }.collect { prefs ->
                _state.update {
                    it.copy(
                        currentColorTheme = prefs.color,
                        isAmoledTheme = prefs.amoled,
                        isDarkTheme = prefs.dark,
                        currentFontTheme = prefs.font
                    )
                }
            }
        }
    }

    /**
     * Observes app state for rate limit information.
     */
    private fun observeAppState() {
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
    }

    /**
     * Performs initial data migration and update checks.
     * Runs in background without blocking UI.
     */
    private fun performInitialMigration() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Migrate legacy data format
                migrateInstalledAppsDataUseCase()
                
                // Check for updates after migration
                installedAppsRepository.checkAllForUpdates()
            } catch (e: Exception) {
                // Silent fail - non-critical operation
                Logger.e(e) { "Initial migration failed" }
            }
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

/**
 * Data class to hold theme preferences for combined flow.
 */
private data class ThemePreferences(
    val color: AppTheme,
    val amoled: Boolean,
    val dark: Boolean,
    val font: FontTheme
)
