package zed.rainxch.githubstore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.githubstore.app.state.components.RateLimitDialog
import zed.rainxch.githubstore.app.navigation.AppNavigation
import zed.rainxch.githubstore.app.navigation.GithubStoreGraph
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.core.presentation.utils.ApplyAndroidSystemBars

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App(
    onAuthenticationChecked: () -> Unit = { },
) {
    val viewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val navBackStack = rememberSerializable(
        serializer = SnapshotStateListSerializer<GithubStoreGraph>()
    ) {
        mutableStateListOf(GithubStoreGraph.HomeScreen)
    }˜

    GithubStoreTheme(
        fontTheme = state.currentFontTheme,
        appTheme = state.currentColorTheme,
        isAmoledTheme = state.isAmoledTheme,
        isDarkTheme = state.isDarkTheme ?: isSystemInDarkTheme()
    ) {
        ApplyAndroidSystemBars(state.isDarkTheme)

        LaunchedEffect(state.isCheckingAuth) {
            if (!state.isCheckingAuth) {
                onAuthenticationChecked()
            }
        }

        if (state.isCheckingAuth) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }

            return@GithubStoreTheme
        }


        if (state.showRateLimitDialog && state.rateLimitInfo != null) {
            RateLimitDialog(
                rateLimitInfo = state.rateLimitInfo,
                isAuthenticated = state.isLoggedIn,
                onDismiss = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)
                },
                onSignIn = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)

                    navBackStack.clear()
                    navBackStack.add(GithubStoreGraph.AuthenticationScreen)
                }
            )
        }

        AppNavigation(
            navBackStack = navBackStack
        )
    }
}