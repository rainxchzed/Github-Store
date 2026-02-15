package zed.rainxch.settings.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fletchmckee.liquid.liquefiable
import zed.rainxch.githubstore.core.presentation.res.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.presentation.locals.LocalBottomNavigationLiquid
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.settings.presentation.components.LogoutDialog
import zed.rainxch.settings.presentation.components.sections.about
import zed.rainxch.settings.presentation.components.sections.appearance
import zed.rainxch.settings.presentation.components.sections.logout

@Composable
fun SettingsRoot(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.OnLogoutSuccessful -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.logout_success))

                    onNavigateBack()
                }
            }

            is SettingsEvent.OnLogoutError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SettingsAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
        snackbarState = snackbarState
    )

    if (state.isLogoutDialogVisible) {
        LogoutDialog(
            onDismissRequest = {
                viewModel.onAction(SettingsAction.OnLogoutDismiss)
            },
            onLogout = {
                viewModel.onAction(SettingsAction.OnLogoutConfirmClick)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    snackbarState: SnackbarHostState
) {
    val liquidState = LocalBottomNavigationLiquid.current
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        },
        topBar = {
            TopAppBar(onAction)
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.liquefiable(liquidState)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            appearance(
                selectedThemeColor = state.selectedThemeColor,
                onThemeColorSelected = { theme ->
                    onAction(SettingsAction.OnThemeColorSelected(theme))
                },
                isAmoledThemeEnabled = state.isAmoledThemeEnabled,
                onAmoledThemeToggled = { enabled ->
                    onAction(SettingsAction.OnAmoledThemeToggled(enabled))
                },
                isDarkTheme = state.isDarkTheme,
                onDarkThemeChange = { isDarkTheme ->
                    onAction(SettingsAction.OnDarkThemeChange(isDarkTheme))
                },
                isUsingSystemFont = state.selectedFontTheme == FontTheme.SYSTEM,
                onUseSystemFontToggled = { enabled ->
                    onAction(
                        SettingsAction.OnFontThemeSelected(
                            if (enabled) {
                                FontTheme.SYSTEM
                            } else FontTheme.CUSTOM
                        )
                    )
                }
            )

            item {
                Spacer(Modifier.height(16.dp))
            }

            about(
                versionName = state.versionName,
                onAction = onAction
            )

            if (state.isUserLoggedIn) {
                item {
                    Spacer(Modifier.height(16.dp))
                }

                logout(
                    onAction = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopAppBar(onAction: (SettingsAction) -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    onAction(SettingsAction.OnNavigateBackClick)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.navigate_back),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        SettingsScreen(
            state = SettingsState(),
            onAction = {},
            snackbarState = SnackbarHostState()
        )
    }
}