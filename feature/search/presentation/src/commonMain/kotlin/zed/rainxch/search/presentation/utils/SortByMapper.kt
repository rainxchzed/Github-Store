package zed.rainxch.search.presentation.utils

import githubstore.feature.search.presentation.generated.resources.Res
import githubstore.feature.search.presentation.generated.resources.sort_best_match
import githubstore.feature.search.presentation.generated.resources.sort_most_forks
import githubstore.feature.search.presentation.generated.resources.sort_most_stars
import org.jetbrains.compose.resources.StringResource
import zed.rainxch.domain.model.SortBy
import zed.rainxch.domain.model.SortBy.BestMatch
import zed.rainxch.domain.model.SortBy.MostForks
import zed.rainxch.domain.model.SortBy.MostStars

fun SortBy.label(): StringResource = when (this) {
    MostStars -> Res.string.sort_most_stars
    MostForks -> Res.string.sort_most_forks
    BestMatch -> Res.string.sort_best_match
}