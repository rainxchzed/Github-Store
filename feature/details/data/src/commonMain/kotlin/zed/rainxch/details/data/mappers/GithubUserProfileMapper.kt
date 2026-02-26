package zed.rainxch.details.data.mappers

import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.details.data.model.GithubUserProfileDto

fun GithubUserProfileDto.toDomain(): GithubUserProfile {
    return GithubUserProfile(
        id = id,
        login = login,
        name = name,
        bio = bio,
        avatarUrl = avatarUrl,
        htmlUrl = htmlUrl,
        followers = followers,
        following = following,
        publicRepos = publicRepos,
        location = location,
        company = company,
        blog = blog,
        twitterUsername = twitterUsername
    )
}