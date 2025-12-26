package zed.rainxch.githubstore.feature.home.data.repository.mappers

import zed.rainxch.githubstore.core.data.model.GithubOwnerNetworkModel
import zed.rainxch.githubstore.core.data.model.GithubRepoNetworkModel
import zed.rainxch.githubstore.feature.home.data.repository.dto.GitLabProjectNetworkModel

fun GitLabProjectNetworkModel.toGithubRepoNetworkModel(): GithubRepoNetworkModel {
    return GithubRepoNetworkModel(
        id = this.id.toLong(),
        name = this.name,
        fullName = this.pathWithNamespace,
        owner = GithubOwnerNetworkModel(
            id = this.namespace?.id?.toLong() ?: 0L,
            login = this.namespace?.path ?: "",
            avatarUrl = this.namespace?.avatarUrl ?: "",
            htmlUrl = this.namespace?.let { "https://gitlab.com/${it.fullPath}" } ?: ""
        ),
        description = this.description,
        defaultBranch = "main",
        htmlUrl = this.webUrl,
        stargazersCount = this.starCount,
        forksCount = this.forksCount,
        language = null,
        topics = this.topics,
        releasesUrl = "https://gitlab.com/api/v4/projects/${this.pathWithNamespace.replace("/", "%2F")}/releases",
        updatedAt = this.lastActivityAt
    )
}