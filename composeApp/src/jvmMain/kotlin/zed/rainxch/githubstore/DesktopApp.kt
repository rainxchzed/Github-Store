package zed.rainxch.githubstore

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import zed.rainxch.githubstore.app.di.initKoin
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.app_icon

fun main() = application {
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "GitHub Store",
        icon = painterResource(Res.drawable.app_icon)
    ) {
        App()
    }
}