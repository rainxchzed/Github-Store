package zed.rainxch.githubstore.core.presentation.model

import zed.rainxch.core.domain.model.GithubRepoSummary

data class DiscoveryRepository(
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val isFavourite: Boolean,
    val isStarred: Boolean,
    val repository: GithubRepoSummary,
)