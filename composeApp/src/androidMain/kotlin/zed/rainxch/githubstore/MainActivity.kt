package zed.rainxch.githubstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import zed.rainxch.githubstore.core.presentation.utils.AppContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var shouldShowSplashScreen = true

        installSplashScreen().setKeepOnScreenCondition {
            shouldShowSplashScreen
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppContextHolder.appContext = applicationContext

        setContent {
            App(
                onAuthenticationChecked = {
                    shouldShowSplashScreen = false
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}