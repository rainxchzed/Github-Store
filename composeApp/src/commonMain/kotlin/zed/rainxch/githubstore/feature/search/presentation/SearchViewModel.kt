package zed.rainxch.githubstore.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.no_repositories_found
import githubstore.composeapp.generated.resources.search_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.feature.search.domain.repository.SearchRepository

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private var currentSearchJob: Job? = null
    private var currentPage = 1
    private var searchDebounceJob: Job? = null

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    init {
        observeInstalledApps()
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            installedAppsRepository.getAllInstalledApps().collect { installedApps ->
                val installedMap = installedApps.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repositories = current.repositories.map { searchRepo ->
                            val app = installedMap[searchRepo.repo.id]
                            searchRepo.copy(
                                isInstalled = app != null,
                                isUpdateAvailable = app?.isUpdateAvailable ?: false
                            )
                        }
                    )
                }
            }
        }
    }

    private fun performSearch(isInitial: Boolean = false) {
        if (_state.value.query.isBlank()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    repositories = emptyList(),
                    errorMessage = null
                )
            }
            return
        }

        if (isInitial) {
            currentSearchJob?.cancel()
            currentPage = 1
        }

        currentSearchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = isInitial,
                    isLoadingMore = !isInitial,
                    errorMessage = null,
                    repositories = if (isInitial) emptyList() else it.repositories
                )
            }

            try {
                val installedAppsSnapshot = installedAppsRepository.getAllInstalledApps().first()
                val installedMap = installedAppsSnapshot.associateBy { it.repoId }

                searchRepository
                    .searchRepositories(
                        query = _state.value.query,
                        searchPlatformType = _state.value.selectedSearchPlatformType,
                        language = _state.value.selectedLanguage,
                        page = currentPage
                    )
                    .collect { paginatedRepos ->
                        currentPage = paginatedRepos.nextPageIndex

                        val newReposWithStatus = coroutineScope {
                            paginatedRepos.repos.map { repo ->
                                async(Dispatchers.IO) {
                                    val app = installedMap[repo.id]
                                    val isUpdateAvailable = if (app?.packageName != null) {
                                        installedAppsRepository.checkForUpdates(app.packageName)
                                    } else false

                                    SearchRepo(
                                        isInstalled = app != null,
                                        isUpdateAvailable = isUpdateAvailable,
                                        repo = repo
                                    )
                                }
                            }.awaitAll()
                        }

                        _state.update { currentState ->
                            val mergedMap = LinkedHashMap<Long, SearchRepo>()

                            currentState.repositories.forEach { r ->
                                mergedMap[r.repo.id] = r
                            }

                            newReposWithStatus.forEach { r ->
                                val existing = mergedMap[r.repo.id]
                                if (existing == null) {
                                    mergedMap[r.repo.id] = r
                                } else {
                                    mergedMap[r.repo.id] = existing.copy(
                                        isInstalled = r.isInstalled,
                                        isUpdateAvailable = r.isUpdateAvailable,
                                        repo = r.repo
                                    )
                                }
                            }

                            val allRepos = mergedMap.values.toList()

                            currentState.copy(
                                repositories = allRepos,
                                hasMorePages = paginatedRepos.hasMore,
                                totalCount = allRepos.size,
                                errorMessage = if (allRepos.isEmpty() && !paginatedRepos.hasMore) {
                                    getString(Res.string.no_repositories_found)
                                } else null
                            )
                        }
                    }

                _state.update {
                    it.copy(isLoading = false, isLoadingMore = false)
                }
            } catch (e: CancellationException) {
                Logger.d { "Search cancelled (expected): ${e.message}" }
            } catch (e: Exception) {
                Logger.e { "Search failed: ${e.message}" }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message ?: getString(Res.string.search_failed)
                    )
                }
            }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnPlatformTypeSelected -> {
                if (_state.value.selectedSearchPlatformType != action.searchPlatformType) {
                    _state.update {
                        it.copy(selectedSearchPlatformType = action.searchPlatformType)
                    }
                    currentPage = 1
                    searchDebounceJob?.cancel()
                    performSearch(isInitial = true)
                }
            }

            is SearchAction.OnLanguageSelected -> {
                if (_state.value.selectedLanguage != action.language) {
                    _state.update {
                        it.copy(
                            selectedLanguage = action.language
                        )
                    }
                    currentPage = 1
                    searchDebounceJob?.cancel()
                    performSearch(isInitial = true)
                }
            }

            is SearchAction.OnRepositoryClick -> {}
            SearchAction.OnNavigateBackClick -> {}

            is SearchAction.OnSearchChange -> {
                _state.update { it.copy(query = action.query) }

                searchDebounceJob?.cancel()

                if (action.query.isBlank()) {
                    _state.update {
                        it.copy(
                            repositories = emptyList(),
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = null
                        )
                    }
                } else {
                    searchDebounceJob = viewModelScope.launch {
                        try {
                            delay(500)
                            currentPage = 1
                            performSearch(isInitial = true)
                        } catch (_: CancellationException) {
                            Logger.d { "Debounce cancelled (expected)" }
                        }
                    }
                }
            }

            SearchAction.OnToggleLanguageSheetVisibility -> {
                _state.update {
                    it.copy(
                        isLanguageSheetVisible = !it.isLanguageSheetVisible
                    )
                }
            }

            SearchAction.OnSearchImeClick -> {
                searchDebounceJob?.cancel()
                currentPage = 1
                performSearch(isInitial = true)
            }

            is SearchAction.OnSortBySelected -> {
                if (_state.value.selectedSortBy != action.sortBy) {
                    _state.update {
                        it.copy(selectedSortBy = action.sortBy)
                    }
                    currentPage = 1
                    searchDebounceJob?.cancel()
                    performSearch(isInitial = true)
                }
            }

            SearchAction.LoadMore -> {
                if (!_state.value.isLoadingMore && _state.value.hasMorePages) {
                    performSearch(isInitial = false)
                }
            }

            SearchAction.Retry -> {
                currentPage = 1
                searchDebounceJob?.cancel()
                performSearch(isInitial = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
        searchDebounceJob?.cancel()
    }
}