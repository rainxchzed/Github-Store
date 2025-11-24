package zed.rainxch.githubstore.feature.home.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.core.domain.model.GithubUser

@Serializable
data class GithubRepoSearchResponse(val items: List<GithubRepoNetworkModel>)

@Serializable
data class GithubRepoNetworkModel(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: GithubOwnerNetworkModel,
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("stargazers_count") val stargazersCount: Int,
    @SerialName("forks_count") val forksCount: Int,
    val language: String? = null,
    val topics: List<String>? = null,
    @SerialName("releases_url") val releasesUrl: String
)

@Serializable
data class GithubOwnerNetworkModel(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val htmlUrl: String
)

fun GithubRepoNetworkModel.toSummary(): GithubRepoSummary = GithubRepoSummary(
    id = id,
    name = name,
    fullName = fullName,
    owner = GithubUser(
        login = owner.login,
        avatarUrl = owner.avatarUrl,
        htmlUrl = owner.htmlUrl
    ),
    description = description,
    htmlUrl = htmlUrl,
    stargazersCount = stargazersCount,
    forksCount = forksCount,
    language = language,
    topics = topics,
    releasesUrl = releasesUrl
)

