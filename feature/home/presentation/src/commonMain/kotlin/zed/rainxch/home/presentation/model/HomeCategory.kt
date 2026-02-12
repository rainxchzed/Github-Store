package zed.rainxch.home.presentation.model

import androidx.compose.runtime.Composable
import githubstore.feature.home.presentation.generated.resources.Res
import githubstore.feature.home.presentation.generated.resources.home_category_new
import githubstore.feature.home.presentation.generated.resources.home_category_recently_updated
import githubstore.feature.home.presentation.generated.resources.home_category_trending
import org.jetbrains.compose.resources.stringResource

enum class HomeCategory {
    TRENDING,
    NEW,
    RECENTLY_UPDATED;

    @Composable
    fun displayText(): String {
        return when (this) {
            TRENDING -> stringResource(Res.string.home_category_trending)
            NEW -> stringResource(Res.string.home_category_new)
            RECENTLY_UPDATED -> stringResource(Res.string.home_category_recently_updated)
        }
    }
}