package zed.rainxch.githubstore.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubRepoSummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: GithubUser,
    val description: String?,
    val htmlUrl: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val topics: List<String>?,
    val releasesUrl: String // Template URL, usually: "https://api.github.com/repos/{owner}/{repo}/releases{/id}"
)