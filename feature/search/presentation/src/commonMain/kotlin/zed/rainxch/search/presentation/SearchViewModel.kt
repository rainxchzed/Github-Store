package zed.rainxch.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import zed.rainxch.githubstore.core.presentation.res.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.RateLimitException
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.core.presentation.model.DiscoveryRepository
import zed.rainxch.domain.repository.SearchRepository

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val logger: GitHubStoreLogger,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var currentSearchJob: Job? = null
    private var currentPage = 1
    private var searchDebounceJob: Job? = null

    private val _state = MutableStateFlow(SearchState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                syncSystemState()

                observeInstalledApps()
                observeFavouriteApps()
                observeStarredRepos()

                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SearchState()
        )

    private fun syncSystemState() {
        viewModelScope.launch {
            try {
                val result = syncInstalledAppsUseCase()
                if (result.isFailure) {
                    logger.warn("Initial sync had issues: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logger.error("Initial sync failed: ${e.message}")
            }
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            installedAppsRepository.getAllInstalledApps().collect { installedApps ->
                val installedMap = installedApps.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repositories = current.repositories.map { searchRepo ->
                            val app = installedMap[searchRepo.repository.id]
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

    private fun observeFavouriteApps() {
        viewModelScope.launch {
            favouritesRepository.getAllFavorites().collect { favoriteRepos ->
                val installedMap = favoriteRepos.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repositories = current.repositories.map { searchRepo ->
                            val app = installedMap[searchRepo.repository.id]
                            searchRepo.copy(
                                isFavourite = app != null
                            )
                        }
                    )
                }
            }
        }
    }

    private fun observeStarredRepos() {
        viewModelScope.launch {
            starredRepository.getAllStarred().collect { starredRepos ->
                val installedMap = starredRepos.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repositories = current.repositories.map { searchRepo ->
                            val app = installedMap[searchRepo.repository.id]
                            searchRepo.copy(isStarred = app != null)
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
                val installedMap = installedAppsRepository
                    .getAllInstalledApps()
                    .first()
                    .associateBy { it.repoId }
                val favoritesMap = favouritesRepository
                    .getAllFavorites()
                    .first()
                    .associateBy { it.repoId }
                val starredReposMap = starredRepository
                    .getAllStarred()
                    .first()
                    .associateBy { it.repoId }

                searchRepository
                    .searchRepositories(
                        query = _state.value.query,
                        searchPlatform = _state.value.selectedSearchPlatform,
                        language = _state.value.selectedLanguage,
                        page = currentPage
                    )
                    .collect { paginatedRepos ->
                        currentPage = paginatedRepos.nextPageIndex

                        val newReposWithStatus = paginatedRepos.repos.map { repo ->
                            val app = installedMap[repo.id]
                            val favourite = favoritesMap[repo.id]
                            val starred = starredReposMap[repo.id]

                            DiscoveryRepository(
                                isInstalled = app != null,
                                isFavourite = favourite != null,
                                isStarred = starred != null,
                                isUpdateAvailable = app?.isUpdateAvailable ?: false,
                                repository = repo
                            )
                        }

                        _state.update { currentState ->
                            val mergedMap = LinkedHashMap<Long, DiscoveryRepository>()

                            currentState.repositories.forEach { r ->
                                mergedMap[r.repository.id] = r
                            }

                            newReposWithStatus.forEach { r ->
                                val existing = mergedMap[r.repository.id]
                                if (existing == null) {
                                    mergedMap[r.repository.id] = r
                                } else {
                                    mergedMap[r.repository.id] = existing.copy(
                                        isInstalled = r.isInstalled,
                                        isUpdateAvailable = r.isUpdateAvailable,
                                        isFavourite = r.isFavourite,
                                        isStarred = r.isStarred,
                                        repository = r.repository
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
            } catch (e: RateLimitException) {
                logger.debug("Rate limit exceeded: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message
                    )
                }

            } catch (e: CancellationException) {
                logger.debug("Search cancelled (expected): ${e.message}")
            } catch (e: Exception) {
                logger.error("Search failed: ${e.message}")
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
                if (_state.value.selectedSearchPlatform != action.searchPlatform) {
                    _state.update {
                        it.copy(selectedSearchPlatform = action.searchPlatform)
                    }
                    currentPage = 1
                    searchDebounceJob?.cancel()
                    performSearch(isInitial = true)
                }
            }

            is SearchAction.OnLanguageSelected -> {
                if (_state.value.selectedLanguage != action.language) {
                    _state.update {
                        it.copy(selectedLanguage = action.language)
                    }
                    currentPage = 1
                    searchDebounceJob?.cancel()
                    performSearch(isInitial = true)
                }
            }


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
                            logger.debug("Debounce cancelled (expected)")
                        }
                    }
                }
            }

            SearchAction.OnToggleLanguageSheetVisibility -> {
                _state.update {
                    it.copy(isLanguageSheetVisible = !it.isLanguageSheetVisible)
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

            is SearchAction.OnRepositoryClick -> {
                /* Handled in composable */
            }

            SearchAction.OnNavigateBackClick -> {
                /* Handled in composable */
            }

            is SearchAction.OnRepositoryDeveloperClick -> {
                /* Handled in composable */
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
        searchDebounceJob?.cancel()
    }
}