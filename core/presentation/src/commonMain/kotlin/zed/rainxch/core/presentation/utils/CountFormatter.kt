package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import githubstore.core.presentation.generated.resources.Res
import githubstore.core.presentation.generated.resources.count_millions
import githubstore.core.presentation.generated.resources.count_thousands
import org.jetbrains.compose.resources.stringResource

@Composable
fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> stringResource(Res.string.count_millions, count / 1_000_000)
        count >= 1000 -> stringResource(Res.string.count_thousands, count / 1000)
        else -> count.toString()
    }
}