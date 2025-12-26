package zed.rainxch.githubstore.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.ClipboardHelper
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthenticationRepository

class AuthenticationViewModel(
    private val apiPlatform: ApiPlatform,
    private val authenticationRepository: AuthenticationRepository,
    private val browserHelper: BrowserHelper,
    private val clipboardHelper: ClipboardHelper,
    private val scope: CoroutineScope,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state: MutableStateFlow<AuthenticationState> =
        MutableStateFlow(AuthenticationState())

    private val _events = Channel<AuthenticationEvents>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                _state.update {
                    it.copy(
                        currentApiPlatform = apiPlatform
                    )
                }

                scope.launch {
                    authenticationRepository.accessTokenFlow.collect { token ->
                        _state.update {
                            it.copy(
                                loginState = if (token.isNullOrEmpty()) {
                                    AuthLoginState.LoggedOut
                                } else {
                                    _events.trySend(AuthenticationEvents.OnNavigateToMain)
                                    AuthLoginState.LoggedIn
                                }
                            )
                        }
                    }
                }

                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AuthenticationState()
        )

    fun onAction(action: AuthenticationAction) {
        when (action) {
            is AuthenticationAction.StartLogin -> startLogin()
            is AuthenticationAction.CopyCode -> copyCode(action.start)
            is AuthenticationAction.OpenPlatform -> openPlatform(action.start)
            AuthenticationAction.MarkLoggedIn -> _state.update { it.copy(loginState = AuthLoginState.LoggedIn) }
            AuthenticationAction.MarkLoggedOut -> _state.update { it.copy(loginState = AuthLoginState.LoggedOut) }
            is AuthenticationAction.OnInfo -> {
                _state.update {
                    it.copy(
                        info = action.message
                    )
                }
            }
        }
    }

    private fun startLogin() {
        scope.launch {
            try {
                val start = authenticationRepository.startDeviceFlow()
                _state.update {
                    it.copy(
                        loginState = AuthLoginState.DevicePrompt(start),
                        copied = false
                    )
                }

                clipboardHelper.copy(
                    label = if (apiPlatform == ApiPlatform.Github) {
                        "GitHub Code"
                    } else "GitLab Code",
                    text = start.userCode
                )

                authenticationRepository.awaitDeviceToken(start = start)

                _state.update { it.copy(loginState = AuthLoginState.LoggedIn) }

                _events.trySend(AuthenticationEvents.OnNavigateToMain)
            } catch (_: CancellationException) {
                _state.update { it.copy(loginState = AuthLoginState.Error("Cancelled")) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loginState = AuthLoginState.Error(
                            t.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

    private fun openPlatform(start: DeviceStart) {
        val url = start.verificationUriComplete ?: start.verificationUri

        browserHelper.openUrl(url)
    }

    private fun copyCode(start: DeviceStart) {
        _state.update {
            it.copy(
                loginState = AuthLoginState.DevicePrompt(start),
                copied = true
            )
        }

        clipboardHelper.copy(
            label = if (apiPlatform == ApiPlatform.Github) {
                "GitHub Code"
            } else "GitLab Code",
            text = start.userCode
        )
    }

}