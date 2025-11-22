package zed.rainxch.githubstore.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.feature.auth.data.DeviceStart
import zed.rainxch.githubstore.feature.auth.domain.AwaitDeviceTokenUseCase
import zed.rainxch.githubstore.feature.auth.domain.LogoutUseCase
import zed.rainxch.githubstore.feature.auth.domain.ObserveAccessTokenUseCase
import zed.rainxch.githubstore.feature.auth.domain.StartDeviceFlowUseCase

class AuthenticationViewModel(
    private val startDeviceFlow: StartDeviceFlowUseCase,
    private val awaitDeviceToken: AwaitDeviceTokenUseCase,
    private val logoutUc: LogoutUseCase,
    observeAccessToken: ObserveAccessTokenUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state: MutableStateFlow<AuthenticationState> =
        MutableStateFlow(AuthenticationState())

    private val _events = Channel<AuthenticationEvents>()
    val events = _events.receiveAsFlow()

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                scope.launch {
                    observeAccessToken().collect { token ->
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
            is AuthenticationAction.StartLogin -> startLogin(action.scope)
            is AuthenticationAction.CopyCode -> copyCode(action.start)
            is AuthenticationAction.OpenGitHub -> openGitHub(action.start)
            is AuthenticationAction.Logout -> logout()
            AuthenticationAction.MarkLoggedIn -> _state.update { it.copy(loginState = AuthLoginState.LoggedIn) }
            AuthenticationAction.MarkLoggedOut -> _state.update { it.copy(loginState = AuthLoginState.LoggedOut) }
        }
    }

    private fun startLogin(scopeText: String) {
        scope.launch {
            try {
                val start = startDeviceFlow(scopeText)
                _state.update {
                    it.copy(
                        loginState = AuthLoginState.DevicePrompt(start),
                        copied = false
                    )
                }

                _events.trySend(
                    AuthenticationEvents.CopyToClipboard(
                        "GitHub Code",
                        start.userCode
                    )
                )

                awaitDeviceToken(start)
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

    private fun openGitHub(start: DeviceStart) {
        val url = start.verificationUriComplete ?: start.verificationUri
        _events.trySend(AuthenticationEvents.OpenBrowser(url))
    }

    private fun copyCode(start: DeviceStart) {
        _state.update {
            it.copy(
                loginState = AuthLoginState.DevicePrompt(start),
                copied = true
            )
        }
        _events.trySend(AuthenticationEvents.CopyToClipboard("GitHub Code", start.userCode))
    }

    private fun logout() {
        scope.launch {
            logoutUc()
            _state.update { it.copy(loginState = AuthLoginState.LoggedOut) }
        }
    }

}