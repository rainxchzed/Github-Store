package zed.rainxch.githubstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.core.data.TokenDataSource

class MainViewModel(
    private val tokenDataSource: TokenDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenDataSource
                .tokenFlow
                .drop(1)
                .collect { authInfo ->
                    _state.update {
                        it.copy(
                            isCheckingAuth = false,
                            isLoggedIn = authInfo != null
                        )
                    }

                    Logger.d("MainViewmodel") { authInfo.toString() }
                }
        }
    }
}