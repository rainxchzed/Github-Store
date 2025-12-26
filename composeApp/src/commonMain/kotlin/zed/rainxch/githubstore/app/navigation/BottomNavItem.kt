package zed.rainxch.githubstore.app.navigation

import org.jetbrains.compose.resources.DrawableResource

data class BottomNavItem(
    val title: String,
    val icon: DrawableResource,
    val screen: GithubStoreGraph
)
