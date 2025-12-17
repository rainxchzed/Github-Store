package zed.rainxch.githubstore.feature.search.presentation

import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.feature.search.domain.model.RootFilterType
import zed.rainxch.githubstore.feature.search.domain.model.SearchPlatformType
import zed.rainxch.githubstore.feature.search.domain.model.SortBy

sealed interface SearchAction {
    data class OnPlatformTypeSelected(val searchPlatformType: SearchPlatformType) : SearchAction
    data class OnSearchChange(val query: String) : SearchAction
    data class OnRepositoryClick(val repository: GithubRepoSummary) : SearchAction
    data class OnRootFilterSelected(val rootFilterType: RootFilterType) : SearchAction
    data class OnSortBySelected(val sortBy: SortBy) : SearchAction
    data object OnSearchImeClick : SearchAction
    data object OnNavigateBackClick : SearchAction
    data object LoadMore : SearchAction
    data object Retry : SearchAction
}
