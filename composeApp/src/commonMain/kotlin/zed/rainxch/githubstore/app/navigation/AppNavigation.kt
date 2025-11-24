package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.githubstore.MainViewModel
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationRoot
import zed.rainxch.githubstore.feature.home.presentation.HomeRoot

@Composable
fun AppNavigation(
    onAuthenticationChecked: () -> Unit = { },
    navHostController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = koinViewModel()
) {
    val state by mainViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isCheckingAuth) {
        if (!state.isCheckingAuth) {
            onAuthenticationChecked()
        }
    }

    if (state.isCheckingAuth) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navHostController,
        startDestination = if (state.isLoggedIn) {
            GithubStoreGraph.HomeScreen
        } else GithubStoreGraph.AuthenticationScreen
    ) {
        composable<GithubStoreGraph.HomeScreen> {
            HomeRoot()
        }

        composable<GithubStoreGraph.AuthenticationScreen> {
            AuthenticationRoot(
                onNavigateToHome = {
                    navHostController.navigate(GithubStoreGraph.HomeScreen) {
                        popUpTo(GithubStoreGraph.AuthenticationScreen) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}