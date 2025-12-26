package zed.rainxch.githubstore.feature.details.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitLabUserNetworkModel(
    val id: Int,
    val username: String,
    val name: String,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("web_url") val webUrl: String,
    val location: String? = null,
    val organization: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    val twitter: String? = null
)