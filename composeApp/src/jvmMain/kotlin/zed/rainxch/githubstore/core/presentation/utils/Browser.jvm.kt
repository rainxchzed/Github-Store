package zed.rainxch.githubstore.core.presentation.utils

import java.awt.Desktop
import java.net.URI

actual fun openBrowser(
    url: String,
    onError: (error: String) -> Unit
) {
    val os = System.getProperty("os.name").lowercase()

    try {
        when {
            os.contains("linux") -> {
                // Just execute and don't wait - browser opening is async
                Runtime.getRuntime().exec(arrayOf("xdg-open", url))
            }

            Desktop.isDesktopSupported() && Desktop.getDesktop()
                .isSupported(Desktop.Action.BROWSE) -> {
                Desktop.getDesktop().browse(URI(url))
            }

            else -> {
                onError("Cannot open browser automatically. Please visit: $url")
            }
        }
    } catch (e: Exception) {
        // Only if the command itself fails to execute
        onError("Failed to open browser: ${e.message}. Please visit: $url")
    }
}