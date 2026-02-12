package zed.rainxch.githubstore.app.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute

fun NavBackStackEntry?.getCurrentScreen(): GithubStoreGraph? {
    if (this == null) return null
    val route = destination.route ?: return null

    return when {
        route.startsWith("HomeScreen") -> GithubStoreGraph.HomeScreen
        route.startsWith("SearchScreen") -> GithubStoreGraph.SearchScreen
        route.startsWith("AuthenticationScreen") -> GithubStoreGraph.AuthenticationScreen
        route.startsWith("DetailsScreen") -> toRoute<GithubStoreGraph.DetailsScreen>()
        route.startsWith("DeveloperProfileScreen") -> toRoute<GithubStoreGraph.DeveloperProfileScreen>()
        route.startsWith("SettingsScreen") -> GithubStoreGraph.SettingsScreen
        route.startsWith("FavouritesScreen") -> GithubStoreGraph.FavouritesScreen
        route.startsWith("StarredReposScreen") -> GithubStoreGraph.StarredReposScreen
        route.startsWith("AppsScreen") -> GithubStoreGraph.AppsScreen
        else -> null
    }
}