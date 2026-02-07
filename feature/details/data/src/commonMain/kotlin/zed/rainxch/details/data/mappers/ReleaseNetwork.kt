package zed.rainxch.details.data.mappers

import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.details.data.dto.ReleaseNetwork

fun ReleaseNetwork.toDomain(): GithubRelease = GithubRelease(
    id = id,
    tagName = tagName,
    name = name,
    author = GithubUser(
        id = author.id,
        login = author.login,
        avatarUrl = author.avatarUrl,
        htmlUrl = author.htmlUrl
    ),
    publishedAt = publishedAt ?: createdAt ?: "",
    description = body,
    assets = assets.map { assetNetwork -> assetNetwork.toDomain() },
    tarballUrl = tarballUrl,
    zipballUrl = zipballUrl,
    htmlUrl = htmlUrl
)
