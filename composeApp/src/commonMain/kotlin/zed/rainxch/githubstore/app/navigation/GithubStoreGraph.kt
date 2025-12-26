package zed.rainxch.githubstore.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import zed.rainxch.githubstore.core.domain.model.ApiPlatform

@Serializable
sealed interface GithubStoreGraph : NavKey {
    @Serializable
    data object HomeGithubScreen : GithubStoreGraph

    data object HomeGitLabScreen : GithubStoreGraph

    @Serializable
    data object SearchScreen : GithubStoreGraph

    @Serializable
    data class AuthenticationScreen(
        val apiPlatform: ApiPlatform
    ) : GithubStoreGraph

    @Serializable
    data class DetailsScreen(
        val repositoryId: Long
    ) : GithubStoreGraph

    @Serializable
    data object SettingsScreen : GithubStoreGraph

    @Serializable
    data object AppsScreen : GithubStoreGraph
}