package zed.rainxch.githubstore.feature.search.domain.model

import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.sort_best_match
import githubstore.composeapp.generated.resources.sort_most_forks
import githubstore.composeapp.generated.resources.sort_most_stars
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class SortBy {
    MostStars,
    MostForks,
    BestMatch;

    fun displayText(): String = when (this) {
        MostStars -> "Most Stars"
        MostForks -> "Most Forks"
        BestMatch -> "Best Match"
    }

    fun label(): StringResource = when (this) {
        MostStars -> Res.string.sort_most_stars
        MostForks -> Res.string.sort_most_forks
        BestMatch -> Res.string.sort_best_match
    }

    fun toGithubParams(): Pair<String?, String> = when (this) {
        MostStars -> "stars" to "desc"
        MostForks -> "forks" to "desc"
        BestMatch -> null to "desc"
    }
}