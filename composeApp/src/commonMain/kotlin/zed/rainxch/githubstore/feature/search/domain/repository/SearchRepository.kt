package zed.rainxch.githubstore.feature.search.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.feature.home.domain.model.PaginatedRepos
import zed.rainxch.githubstore.feature.search.domain.model.RootFilterType
import zed.rainxch.githubstore.feature.search.domain.model.SearchPlatformType

interface SearchRepository {
    fun searchRepositories(
        query: String,
        searchPlatformType: SearchPlatformType,
        rootFilterType: RootFilterType,
        page: Int
    ): Flow<PaginatedRepos>
}