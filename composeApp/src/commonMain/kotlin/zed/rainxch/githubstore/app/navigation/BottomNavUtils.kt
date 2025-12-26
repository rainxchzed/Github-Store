package zed.rainxch.githubstore.app.navigation

import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.ic_github
import githubstore.composeapp.generated.resources.ic_gitlab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object BottomNavUtils {
    fun getItems(): ImmutableList<BottomNavItem> {
        return persistentListOf(
            BottomNavItem(
                title = "Github",
                icon = Res.drawable.ic_github,
                screen = GithubStoreGraph.HomeGithubScreen
            ),
            BottomNavItem(
                title = "GitLab",
                icon = Res.drawable.ic_gitlab,
                screen = GithubStoreGraph.HomeGitLabScreen
            )
        )
    }

    fun allowedScreens(): ImmutableList<GithubStoreGraph> {
        return persistentListOf(
            GithubStoreGraph.HomeGithubScreen,
            GithubStoreGraph.HomeGitLabScreen
        )
    }
}