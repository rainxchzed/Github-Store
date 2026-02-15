package zed.rainxch.githubstore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ApplyAndroidSystemBars
import zed.rainxch.githubstore.app.navigation.AppNavigation
import zed.rainxch.githubstore.app.navigation.GithubStoreGraph
import zed.rainxch.githubstore.app.components.RateLimitDialog

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    val viewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val navBackStack = rememberNavController()

    GithubStoreTheme(
        fontTheme = state.currentFontTheme,
        appTheme = state.currentColorTheme,
        isAmoledTheme = state.isAmoledTheme,
        isDarkTheme = state.isDarkTheme ?: isSystemInDarkTheme()
    ) {
        ApplyAndroidSystemBars(state.isDarkTheme)

        if (state.showRateLimitDialog && state.rateLimitInfo != null) {
            RateLimitDialog(
                rateLimitInfo = state.rateLimitInfo,
                isAuthenticated = state.isLoggedIn,
                onDismiss = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)
                },
                onSignIn = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)

                    navBackStack.navigate(GithubStoreGraph.AuthenticationScreen)
                }
            )
        }

        AppNavigation(
            navController = navBackStack
        )
    }
}