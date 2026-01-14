package zed.rainxch.githubstore.app.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.fletchmckee.liquid.rememberLiquidState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.githubstore.feature.apps.presentation.AppsRoot
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationRoot
import zed.rainxch.githubstore.feature.details.presentation.DetailsRoot
import zed.rainxch.githubstore.feature.developer_profile.presentation.DeveloperProfileRoot
import zed.rainxch.githubstore.feature.favourites.presentation.FavouritesRoot
import zed.rainxch.githubstore.feature.home.presentation.HomeRoot
import zed.rainxch.githubstore.feature.search.presentation.SearchRoot
import zed.rainxch.githubstore.feature.settings.presentation.SettingsRoot
import zed.rainxch.githubstore.feature.starred_repos.presentation.StarredReposRoot

@Composable
fun AppNavigation(
    navBackStack: SnapshotStateList<GithubStoreGraph>
) {
    val liquidState = rememberLiquidState()

    CompositionLocalProvider(
        value = LocalBottomNavigationLiquid provides liquidState
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavDisplay(
                backStack = navBackStack,
                onBack = {
                    navBackStack.removeLastOrNull()
                },
                entryProvider = entryProvider {
                    entry<GithubStoreGraph.HomeScreen> {
                        HomeRoot(
                            onNavigateToSearch = {
                                navBackStack.add(GithubStoreGraph.SearchScreen)
                            },
                            onNavigateToSettings = {
                                navBackStack.add(GithubStoreGraph.SettingsScreen)
                            },
                            onNavigateToApps = {
                                navBackStack.add(GithubStoreGraph.AppsScreen)
                            },
                            onNavigateToDetails = { repo ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repo.id
                                    )
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navBackStack.add(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username
                                    )
                                )
                            },
                        )
                    }

                    entry<GithubStoreGraph.SearchScreen> {
                        SearchRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onNavigateToDetails = { repo ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repo.id
                                    )
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navBackStack.add(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username
                                    )
                                )
                            },
                        )
                    }

                    entry<GithubStoreGraph.DetailsScreen> { args ->
                        DetailsRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onOpenRepositoryInApp = { repoId ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId
                                    )
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navBackStack.add(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username
                                    )
                                )
                            },
                            viewModel = koinViewModel {
                                parametersOf(args.repositoryId)
                            }
                        )
                    }

                    entry<GithubStoreGraph.DeveloperProfileScreen> { args ->
                        DeveloperProfileRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onNavigateToDetails = { repoId ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId
                                    )
                                )
                            },
                            viewModel = koinViewModel {
                                parametersOf(args.username)
                            }
                        )
                    }

                    entry<GithubStoreGraph.AuthenticationScreen> {
                        AuthenticationRoot(
                            onNavigateToHome = {
                                navBackStack.clear()
                                navBackStack.add(GithubStoreGraph.HomeScreen)
                            }
                        )
                    }

                    entry<GithubStoreGraph.FavouritesScreen> {
                        FavouritesRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onNavigateToDetails = {
                                navBackStack.add(GithubStoreGraph.DetailsScreen(it))
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navBackStack.add(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username
                                    )
                                )
                            },
                        )
                    }

                    entry<GithubStoreGraph.StarredReposScreen> {
                        StarredReposRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onNavigateToDetails = { repoId ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId
                                    )
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navBackStack.add(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username
                                    )
                                )
                            },
                        )
                    }

                    entry<GithubStoreGraph.SettingsScreen> {
                        SettingsRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            }
                        )
                    }

                    entry<GithubStoreGraph.AppsScreen> {
                        AppsRoot(
                            onNavigateBack = {
                                navBackStack.removeLastOrNull()
                            },
                            onNavigateToRepo = { repoId ->
                                navBackStack.add(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId
                                    )
                                )
                            }
                        )
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    )
                },
                popTransitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    )
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(Spring.DampingRatioLowBouncy)
                    )
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )

            BottomNavigation(
                currentScreen = navBackStack.last(),
                onNavigate = {
                    navBackStack.add(it)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}