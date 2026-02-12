package zed.rainxch.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun isDynamicColorAvailable(): Boolean {
    return false
}

@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    return null
}