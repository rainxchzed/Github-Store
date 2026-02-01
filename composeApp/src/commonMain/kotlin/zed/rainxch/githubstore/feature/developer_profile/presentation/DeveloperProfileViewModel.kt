@file:OptIn(ExperimentalTime::class)

package zed.rainxch.githubstore.feature.developer_profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.failed_to_load_profile
import githubstore.composeapp.generated.resources.failed_to_load_repositories
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.data.local.db.entities.FavoriteRepo
import zed.rainxch.githubstore.core.domain.repository.FavouritesRepository
import zed.rainxch.githubstore.feature.developer_profile.domain.model.RepoFilterType
import zed.rainxch.githubstore.feature.developer_profile.domain.model.RepoSortType
import zed.rainxch.githubstore.feature.developer_profile.domain.repository.DeveloperProfileRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ViewModel for the Developer Profile screen.
 * 
 * Manages loading developer profile and repositories, filtering, sorting,
 * search functionality, and favorite toggling.
 */
class DeveloperProfileViewModel(
    private val username: String,
    private val repository: DeveloperProfileRepository,
    private val favouritesRepository: FavouritesRepository
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(DeveloperProfileState(username = username))
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadDeveloperData()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DeveloperProfileState(username = username)
        )

    /**
     * Loads developer profile and repositories sequentially.
     * Profile is loaded first, then repositories.
     */
    private fun loadDeveloperData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isLoadingRepos = false,
                    errorMessage = null
                )
            }

            // Load profile first
            val profileResult = repository.getDeveloperProfile(username)
            profileResult
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            isLoadingRepos = true
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                                ?: getString(Res.string.failed_to_load_profile)
                        )
                    }
                    return@launch
                }

            // Load repositories
            val reposResult = repository.getDeveloperRepositories(username)
            reposResult
                .onSuccess { repos ->
                    _state.update {
                        it.copy(
                            repositories = repos.toImmutableList(),
                            isLoading = false,
                            isLoadingRepos = false
                        )
                    }
                    applyFiltersAndSort()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingRepos = false,
                            errorMessage = error.message
                                ?: getString(Res.string.failed_to_load_repositories)
                        )
                    }
                }
        }
    }

    /**
     * Applies current filters and sorting to the repository list.
     * Runs on Default dispatcher to avoid blocking UI.
     */
    private fun applyFiltersAndSort() {
        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _state.value
            var filtered = currentState.repositories

            // Apply search filter
            if (currentState.searchQuery.isNotBlank()) {
                val query = currentState.searchQuery.lowercase()
                filtered = filtered.filter { repo ->
                    repo.name.lowercase().contains(query) ||
                            repo.description?.lowercase()?.contains(query) == true
                }.toImmutableList()
            }

            // Apply category filter
            filtered = when (currentState.currentFilter) {
                RepoFilterType.ALL -> filtered
                RepoFilterType.WITH_RELEASES -> filtered.filter { it.hasInstallableAssets }
                    .toImmutableList()
                RepoFilterType.INSTALLED -> filtered.filter { it.isInstalled }
                    .toImmutableList()
                RepoFilterType.FAVORITES -> filtered.filter { it.isFavorite }
                    .toImmutableList()
            }

            // Apply sorting
            filtered = when (currentState.currentSort) {
                RepoSortType.UPDATED -> filtered.sortedByDescending { it.updatedAt }
                    .toImmutableList()
                RepoSortType.STARS -> filtered.sortedByDescending { it.stargazersCount }
                    .toImmutableList()
                RepoSortType.NAME -> filtered.sortedBy { it.name.lowercase() }
                    .toImmutableList()
            }

            _state.update { it.copy(filteredRepositories = filtered) }
        }
    }

    fun onAction(action: DeveloperProfileAction) {
        when (action) {
            // Navigation actions handled by composable
            DeveloperProfileAction.OnNavigateBackClick,
            is DeveloperProfileAction.OnRepositoryClick,
            is DeveloperProfileAction.OnOpenLink -> Unit

            is DeveloperProfileAction.OnFilterChange -> {
                _state.update { it.copy(currentFilter = action.filter) }
                applyFiltersAndSort()
            }

            is DeveloperProfileAction.OnSortChange -> {
                _state.update { it.copy(currentSort = action.sort) }
                applyFiltersAndSort()
            }

            is DeveloperProfileAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
                applyFiltersAndSort()
            }

            is DeveloperProfileAction.OnToggleFavorite -> {
                toggleFavorite(action.repository)
            }

            DeveloperProfileAction.OnDismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            DeveloperProfileAction.OnRetry -> {
                loadDeveloperData()
            }
        }
    }
    
    /**
     * Toggles favorite status for a repository.
     * Updates both local state and database.
     */
    private fun toggleFavorite(repo: zed.rainxch.githubstore.feature.developer_profile.domain.model.DeveloperRepository) {
        viewModelScope.launch {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val repoOwner = repo.fullName.substringBefore('/')
            
            val favoriteRepo = FavoriteRepo(
                repoId = repo.id,
                repoName = repo.name,
                repoOwner = repoOwner,
                repoOwnerAvatarUrl = _state.value.profile?.avatarUrl.orEmpty(),
                repoDescription = repo.description,
                primaryLanguage = repo.language,
                repoUrl = repo.htmlUrl,
                latestVersion = repo.latestVersion,
                latestReleaseUrl = null,
                addedAt = currentTime,
                lastSyncedAt = currentTime
            )

            favouritesRepository.toggleFavorite(favoriteRepo)

            // Update local state
            _state.update { state ->
                val updatedRepos = state.repositories.map {
                    if (it.id == repo.id) {
                        it.copy(isFavorite = !it.isFavorite)
                    } else {
                        it
                    }
                }.toImmutableList()

                state.copy(repositories = updatedRepos)
            }
            
            applyFiltersAndSort()
        }
    }
}
