package zed.rainxch.core.presentation.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

fun gridColumnCount(contentWidthDp: Float, maxCardWidthDp: Float, spacingDp: Float = 10f): Int =
    max(1, ceil((contentWidthDp - spacingDp) / (maxCardWidthDp + spacingDp)).toInt())

object CardGridSpec {
    val InfoMaxCardWidth: Dp = 550.dp
    val LowDensityMaxCardWidth: Dp = 270.dp
    val GridSpacing: Dp = 10.dp
}

@Composable
fun rememberGridColumns(maxCardWidth: Dp): Int {
    var columns by remember { mutableStateOf(1) }
    BoxWithConstraints {
        columns = gridColumnCount(
            contentWidthDp = maxWidth.value,
            maxCardWidthDp = maxCardWidth.value,
            spacingDp = CardGridSpec.GridSpacing.value,
        )
    }
    return columns
}
