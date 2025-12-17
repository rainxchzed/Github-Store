package zed.rainxch.githubstore.feature.search.domain.model

enum class SortBy {
    MostStars,
    MostForks,
    BestMatch;

    fun displayText(): String = when (this) {
        MostStars -> "Most Stars"
        MostForks -> "Most Forks"
        BestMatch -> "Best Match"
    }
}