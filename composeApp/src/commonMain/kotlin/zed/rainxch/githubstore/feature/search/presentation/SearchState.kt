package zed.rainxch.githubstore.feature.search.presentation

import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.feature.search.domain.model.RootFilterType
import zed.rainxch.githubstore.feature.search.domain.model.SearchPlatformType
import zed.rainxch.githubstore.feature.search.domain.model.SortBy

data class SearchState(
    val search: String = "",
    val repositories: List<GithubRepoSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val selectedSearchPlatformType: SearchPlatformType = SearchPlatformType.All,
    val selectedRootFilter: RootFilterType = RootFilterType.All,
    val selectedSortBy: SortBy = SortBy.BestMatch,
    val hasMorePages: Boolean = false,
    val totalCount: Int? = null
)