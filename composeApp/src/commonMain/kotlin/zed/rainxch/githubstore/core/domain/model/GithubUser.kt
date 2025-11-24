package zed.rainxch.githubstore.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubUser(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String
)