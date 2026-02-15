package zed.rainxch.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.PaginatedDiscoveryRepositories
import zed.rainxch.domain.model.ProgrammingLanguage
import zed.rainxch.domain.model.SearchPlatform

interface SearchRepository {
    fun searchRepositories(
        query: String,
        searchPlatform: SearchPlatform,
        language: ProgrammingLanguage,
        page: Int
    ): Flow<PaginatedDiscoveryRepositories>
}