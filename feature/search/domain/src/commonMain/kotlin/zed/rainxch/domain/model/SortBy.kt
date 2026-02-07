package zed.rainxch.domain.model

enum class SortBy {
    MostStars,
    MostForks,
    BestMatch;

    fun toGithubParams(): Pair<String?, String> = when (this) {
        MostStars -> "stars" to "desc"
        MostForks -> "forks" to "desc"
        BestMatch -> null to "desc"
    }
}