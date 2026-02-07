package zed.rainxch.search.presentation

import zed.rainxch.core.presentation.model.DiscoveryRepository
import zed.rainxch.domain.model.ProgrammingLanguage
import zed.rainxch.domain.model.SearchPlatform
import zed.rainxch.domain.model.SortBy

data class SearchState(
    val query: String = "",
    val repositories: List<DiscoveryRepository> = emptyList(),
    val selectedSearchPlatform: SearchPlatform = SearchPlatform.All,
    val selectedSortBy: SortBy = SortBy.BestMatch,
    val selectedLanguage: ProgrammingLanguage = ProgrammingLanguage.All,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMorePages: Boolean = true,
    val totalCount: Int? = null,
    val isLanguageSheetVisible: Boolean = false
)
