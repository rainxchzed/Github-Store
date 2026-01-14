package zed.rainxch.githubstore.feature.starred_repos.presentation.mappers

import zed.rainxch.githubstore.core.data.local.db.entities.StarredRepo
import zed.rainxch.githubstore.feature.starred_repos.presentation.model.StarredRepositoryUi

fun StarredRepo.toStarredRepositoryUi(isFavorite: Boolean = false) = StarredRepositoryUi(
    repoId = repoId,
    repoName = repoName,
    repoOwner = repoOwner,
    repoOwnerAvatarUrl = repoOwnerAvatarUrl,
    repoDescription = repoDescription,
    primaryLanguage = primaryLanguage,
    repoUrl = repoUrl,
    stargazersCount = stargazersCount,
    forksCount = forksCount,
    openIssuesCount = openIssuesCount,
    isInstalled = isInstalled,
    isFavorite = isFavorite,
    latestRelease = latestVersion,
    latestReleaseUrl = latestReleaseUrl,
    starredAt = starredAt
)