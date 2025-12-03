package zed.rainxch.githubstore.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.githubstore.core.data.TokenDataSource
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import zed.rainxch.githubstore.feature.home.presentation.model.HomeCategory

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val tokenDataSource: TokenDataSource
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var currentJob: Job? = null
    private var nextPageIndex = 1

    private val _state = MutableStateFlow(HomeState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadRepos(isInitial = true)
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeState()
        )

    private fun loadRepos(isInitial: Boolean = false, category: HomeCategory? = null) {
        if (_state.value.isLoading || _state.value.isLoadingMore) {
            Logger.d { "Already loading, skipping..." }
            return
        }

        currentJob?.cancel()

        if (isInitial) {
            nextPageIndex = 1
        }

        val targetCategory = category ?: _state.value.currentCategory

        Logger.d { "Loading repos: category=$targetCategory, page=$nextPageIndex, isInitial=$isInitial" }

        currentJob = viewModelScope.launch {
            val token = tokenDataSource.current()

            if (token == null) {
                _state.update {
                    it.copy(
                        needsAuth = true,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    isLoading = isInitial,
                    isLoadingMore = !isInitial,
                    errorMessage = null,
                    needsAuth = false,
                    currentCategory = targetCategory,
                    repos = if (isInitial) emptyList() else it.repos
                )
            }

            try {
                val flow = when (targetCategory) {
                    HomeCategory.POPULAR -> homeRepository.getTrendingRepositories(nextPageIndex)
                    HomeCategory.LATEST_UPDATED -> homeRepository.getLatestUpdated(nextPageIndex)
                    HomeCategory.NEW -> homeRepository.getNew(nextPageIndex)
                }

                flow.collect { paginatedRepos ->
                    Logger.d { "Received ${paginatedRepos.repos.size} repos, hasMore=${paginatedRepos.hasMore}, nextPage=${paginatedRepos.nextPageIndex}" }

                    this@HomeViewModel.nextPageIndex = paginatedRepos.nextPageIndex

                    _state.update { currentState ->
                        val rawList = currentState.repos + paginatedRepos.repos
                        val uniqueList = rawList.distinctBy { it.fullName }

                        currentState.copy(
                            repos = uniqueList,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMorePages = paginatedRepos.hasMore,
                            errorMessage = if (uniqueList.isEmpty() && !paginatedRepos.hasMore) {
                                "No repositories found"
                            } else null
                        )
                    }
                }

                Logger.d { "Flow completed" }
                _state.update {
                    it.copy(isLoading = false, isLoadingMore = false)
                }

            } catch (t: Throwable) {
                if (t is CancellationException) {
                    Logger.d { "Load cancelled (expected)" }
                    throw t
                }

                Logger.e { "Load failed: ${t.message}" }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = t.message ?: "Failed to load repositories"
                    )
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> {
                nextPageIndex = 1
                loadRepos(isInitial = true)
            }

            HomeAction.Retry -> {
                nextPageIndex = 1
                loadRepos(isInitial = true)
            }

            HomeAction.LoadMore -> {
                Logger.d { "LoadMore action: isLoading=${_state.value.isLoading}, isLoadingMore=${_state.value.isLoadingMore}, hasMore=${_state.value.hasMorePages}" }

                if (!_state.value.isLoadingMore && !_state.value.isLoading && _state.value.hasMorePages) {
                    loadRepos(isInitial = false)
                }
            }

            is HomeAction.SwitchCategory -> {
                if (_state.value.currentCategory != action.category) {
                    nextPageIndex = 1
                    loadRepos(isInitial = true, category = action.category)
                }
            }

            is HomeAction.OnRepositoryClick -> {
                /* Handled in composable */
            }

            HomeAction.OnSearchClick -> {
                /* Handled in composable */
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}