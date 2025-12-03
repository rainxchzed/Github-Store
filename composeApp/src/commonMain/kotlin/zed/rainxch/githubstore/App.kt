package zed.rainxch.githubstore

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.githubstore.app.navigation.AppNavigation
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme

@Composable
@Preview
fun App(
    onAuthenticationChecked: () -> Unit = { },
) {
    GithubStoreTheme {
        AppNavigation(
            onAuthenticationChecked = onAuthenticationChecked
        )
    }
}