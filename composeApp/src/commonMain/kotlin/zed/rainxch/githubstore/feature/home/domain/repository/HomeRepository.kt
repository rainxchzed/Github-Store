package zed.rainxch.githubstore.feature.home.domain.repository

import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getTrendingRepositories(page: Int): Flow<PaginatedRepos>
    fun getLatestUpdated(page: Int): Flow<PaginatedRepos>
    fun getNew(page: Int): Flow<PaginatedRepos>
}