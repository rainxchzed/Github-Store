package zed.rainxch.githubstore.core.domain.model

data class GithubRelease(
    val id: Long,
    val tagName: String,
    val name: String?,
    val author: GithubUser,
    val publishedAt: String,
    val description: String?,
    val assets: List<GithubAsset>,
    val tarballUrl: String,
    val zipballUrl: String,
    val htmlUrl: String
)

data class GithubAsset(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val downloadUrl: String,
    val uploader: GithubUser
)