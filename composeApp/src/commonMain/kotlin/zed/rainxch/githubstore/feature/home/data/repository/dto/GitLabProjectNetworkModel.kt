package zed.rainxch.githubstore.feature.home.data.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitLabProjectNetworkModel(
    val id: Int,
    val name: String,
    @SerialName("path_with_namespace") val pathWithNamespace: String,
    val namespace: GitLabNamespaceNetworkModel? = null,
    val description: String? = null,
    @SerialName("web_url") val webUrl: String,
    @SerialName("star_count") val starCount: Int,
    @SerialName("forks_count") val forksCount: Int,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val topics: List<String>? = null,
    @SerialName("last_activity_at") val lastActivityAt: String,
    @SerialName("default_branch") val default_branch: String? = null
)

@Serializable
data class GitLabNamespaceNetworkModel(
    val id: Int,
    val path: String,
    @SerialName("full_path") val fullPath: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class GitLabReleaseNetworkModel(
    val name: String? = null,
    @SerialName("tag_name") val tagName: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("released_at") val releasedAt: String? = null,
    val assets: GitLabReleaseAssets? = null,
    val upcoming_release: Boolean? = null // GitLab's version of prerelease
)

@Serializable
data class GitLabReleaseAssets(
    val count: Int = 0,
    val sources: List<GitLabReleaseSource> = emptyList(),
    val links: List<GitLabReleaseLink> = emptyList()
)

@Serializable
data class GitLabReleaseSource(
    val format: String,
    val url: String
)

@Serializable
data class GitLabReleaseLink(
    val id: Int,
    val name: String,
    val url: String,
    @SerialName("link_type") val linkType: String? = null
)