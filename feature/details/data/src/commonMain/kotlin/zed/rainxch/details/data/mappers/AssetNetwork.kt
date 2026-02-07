package zed.rainxch.details.data.mappers

import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.details.data.dto.AssetNetwork

fun AssetNetwork.toDomain(): GithubAsset = GithubAsset(
    id = id,
    name = name,
    contentType = contentType,
    size = size,
    downloadUrl = downloadUrl,
    uploader = GithubUser(
        id = uploader.id,
        login = uploader.login,
        avatarUrl = uploader.avatarUrl,
        htmlUrl = uploader.htmlUrl
    )
)