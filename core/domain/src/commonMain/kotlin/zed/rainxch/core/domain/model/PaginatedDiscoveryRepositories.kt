package zed.rainxch.core.domain.model

data class PaginatedDiscoveryRepositories(
    val repos: List<GithubRepoSummary>,
    val hasMore: Boolean,
    val nextPageIndex: Int,
    val totalCount: Int? = null
)