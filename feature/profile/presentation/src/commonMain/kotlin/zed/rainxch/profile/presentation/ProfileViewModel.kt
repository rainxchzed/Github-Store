package zed.rainxch.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.repository.ThemesRepository
import zed.rainxch.core.domain.utils.BrowserHelper
import zed.rainxch.settings.domain.repository.SettingsRepository

class ProfileViewModel(
    private val browserHelper: BrowserHelper,
    private val themesRepository: ThemesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ProfileState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadCurrentTheme()
                collectIsUserLoggedIn()
                loadVersionName()

                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ProfileState()
        )

    private val _events = Channel<ProfileEvent>()
    val events = _events.receiveAsFlow()

    private fun loadVersionName() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    versionName = settingsRepository.getVersionName()
                )
            }
        }
    }

    private fun collectIsUserLoggedIn() {
        viewModelScope.launch {
            settingsRepository.isUserLoggedIn
                .collect { isLoggedIn ->
                    _state.update { it.copy(isUserLoggedIn = isLoggedIn) }
                }
        }
    }

    private fun loadCurrentTheme() {
        viewModelScope.launch {
            themesRepository.getThemeColor().collect { theme ->
                _state.update {
                    it.copy(selectedThemeColor = theme)
                }
            }
        }

        viewModelScope.launch {
            themesRepository.getAmoledTheme().collect { isAmoled ->
                _state.update {
                    it.copy(isAmoledThemeEnabled = isAmoled)
                }
            }
        }

        viewModelScope.launch {
            themesRepository.getIsDarkTheme().collect { isDarkTheme ->
                _state.update {
                    it.copy(isDarkTheme = isDarkTheme)
                }
            }
        }

        viewModelScope.launch {
            themesRepository.getFontTheme().collect { fontTheme ->
                _state.update {
                    it.copy(selectedFontTheme = fontTheme)
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OnHelpClick -> {
                browserHelper.openUrl(
                    url = "https://github.com/rainxchzed/Github-Store/issues"
                )
            }

            is ProfileAction.OnThemeColorSelected -> {
                viewModelScope.launch {
                    themesRepository.setThemeColor(action.themeColor)
                }
            }

            is ProfileAction.OnAmoledThemeToggled -> {
                viewModelScope.launch {
                    themesRepository.setAmoledTheme(action.enabled)
                }
            }

            ProfileAction.OnLogoutClick -> {
                _state.update {
                    it.copy(
                        isLogoutDialogVisible = true
                    )
                }
            }

            ProfileAction.OnLogoutConfirmClick -> {
                viewModelScope.launch {
                    runCatching {
                        settingsRepository.logout()
                    }.onSuccess {
                        _state.update { it.copy(isLogoutDialogVisible = false) }
                        _events.send(ProfileEvent.OnLogoutSuccessful)
                    }.onFailure { error ->
                        _state.update { it.copy(isLogoutDialogVisible = false) }
                        error.message?.let {
                            _events.send(ProfileEvent.OnLogoutError(it))
                        }
                    }
                }
            }

            ProfileAction.OnLogoutDismiss -> {
                _state.update {
                    it.copy(
                        isLogoutDialogVisible = false
                    )
                }
            }

            ProfileAction.OnNavigateBackClick -> {
                /* Handed in composable */
            }

            is ProfileAction.OnFontThemeSelected -> {
                viewModelScope.launch {
                    themesRepository.setFontTheme(action.fontTheme)
                }
            }

            is ProfileAction.OnDarkThemeChange -> {
                viewModelScope.launch {
                    themesRepository.setDarkTheme(action.isDarkTheme)
                }
            }
        }
    }

}