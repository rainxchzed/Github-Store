package zed.rainxch.home.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CachedGithubRepoSummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: CachedGithubOwner,
    val description: String?,
    val defaultBranch: String,
    val htmlUrl: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val topics: List<String>?,
    val releasesUrl: String,
    val updatedAt: String,
    val createdAt: String? = null,
    val latestReleaseDate: String? = null,
    val releaseRecency: Int? = null,
    val releaseRecencyText: String? = null,
    val trendingScore: Double? = null,
    val popularityScore: Int? = null
)