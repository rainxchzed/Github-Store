package zed.rainxch.githubstore.feature.details.domain.repository

import zed.rainxch.githubstore.core.domain.model.GithubRelease
import zed.rainxch.githubstore.core.domain.model.GithubRepoSummary
import zed.rainxch.githubstore.core.domain.model.GithubUserProfile
import zed.rainxch.githubstore.feature.details.domain.model.RepoStats

interface DetailsRepository {
    suspend fun getRepositoryById(id: Long): GithubRepoSummary

    suspend fun getLatestPublishedRelease(owner: String, repo: String): GithubRelease?

    suspend fun getReadme(owner: String, repo: String): String?

    suspend fun getRepoStats(owner: String, repo: String): RepoStats

    suspend fun getUserProfile(username: String): GithubUserProfile // ADD THIS
}