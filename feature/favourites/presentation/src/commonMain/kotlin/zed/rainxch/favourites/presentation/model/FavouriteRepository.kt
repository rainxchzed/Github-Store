package zed.rainxch.favourites.presentation.model

data class FavouriteRepository(
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
    val repoDescription: String?,
    val primaryLanguage: String?,
    val repoUrl: String,
    val addedAtFormatter: String,
    val latestRelease: String?,
    val latestReleaseUrl: String?,
)
