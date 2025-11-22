package zed.rainxch.githubstore

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationRoot

@Composable
@Preview
fun App() {
    GithubStoreTheme {
        AuthenticationRoot(
            onNavigateToMain = {

            }
        )
    }
}