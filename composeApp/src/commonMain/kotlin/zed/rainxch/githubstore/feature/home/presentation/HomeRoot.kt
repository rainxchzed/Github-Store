package zed.rainxch.githubstore.feature.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.app_icon
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.githubstore.app.navigation.locals.LocalBottomNavLiquidState
import zed.rainxch.githubstore.core.presentation.components.GithubStoreButton
import zed.rainxch.githubstore.core.presentation.components.RepositoryCard
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.core.presentation.utils.isLiquidTopbarEnabled
import zed.rainxch.githubstore.feature.home.presentation.components.HomeFilterChips
import zed.rainxch.githubstore.feature.home.presentation.model.HomeCategory

@Composable
fun HomeRoot(
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToDetails: (zed.rainxch.githubstore.core.domain.model.GithubRepoSummary) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                HomeAction.OnSearchClick -> {
                    onNavigateToSearch()
                }

                HomeAction.OnSettingsClick -> {
                    onNavigateToSettings()
                }

                HomeAction.OnAppsClick -> {
                    onNavigateToApps()
                }

                is HomeAction.OnRepositoryClick -> onNavigateToDetails(action.repo)

                else -> {
                    viewModel.onAction(action)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState()
    val liquidState = LocalBottomNavLiquidState.current

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            totalItems > 0 &&
                    lastVisibleItem != null &&
                    lastVisibleItem.index >= (totalItems - 5) &&
                    !state.isLoadingMore &&
                    !state.isLoading &&
                    state.hasMorePages
        }
    }

    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            currentOnAction(HomeAction.LoadMore)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                title = {
                    Text(
                        text = "Github Store",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                actions = {
                    topbarActions(
                        state = state,
                        onAction = onAction
                    )
                },
                modifier = Modifier.padding(12.dp)
            )
        },
        modifier = Modifier.liquefiable(liquidState),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer, CircleShape
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeCategory.entries.forEach { category ->
                    HomeFilterChips(
                        selectedCategory = state.currentCategory,
                        category = category,
                        onClick = {
                            onAction(HomeAction.SwitchCategory(category))
                        }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (state.isLoading && state.repos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularWavyProgressIndicator()

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Finding repositories...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (state.errorMessage != null && state.repos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            GithubStoreButton(
                                text = "Retry",
                                onClick = {
                                    onAction(HomeAction.Retry)
                                }
                            )
                        }
                    }
                }

                if (state.repos.isNotEmpty()) {
                    LazyVerticalStaggeredGrid(
                        state = listState,
                        columns = StaggeredGridCells.Adaptive(350.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.repos,
                            key = { it.repo.id },
                            contentType = { "repo" }
                        ) { homeRepo ->
                            RepositoryCard(
                                isInstalled = homeRepo.isInstalled,
                                isUpdateAvailable = homeRepo.isUpdateAvailable,
                                repository = homeRepo.repo,
                                onClick = {
                                    onAction(HomeAction.OnRepositoryClick(homeRepo.repo))
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .liquefiable(liquidState)
                            )
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "Loading more...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // End message
                        if (!state.hasMorePages && !state.isLoadingMore) {
                            item(key = "end_message") {
                                Text(
                                    text = "No more repositories",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun topbarActions(
    state: HomeState,
    onAction: (HomeAction) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            shapes = IconButtonDefaults.shapes(),
            onClick = {
                onAction(HomeAction.OnSearchClick)
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(24.dp)
            )
        }

        if (state.isAppsSectionVisible) {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    onAction(HomeAction.OnAppsClick)
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Apps",
                        modifier = Modifier.size(24.dp)
                    )

                    if (state.isUpdateAvailable) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .padding(top = 8.dp, end = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        IconButton(
            shapes = IconButtonDefaults.shapes(),
            onClick = {
                onAction(HomeAction.OnSettingsClick)
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        HomeScreen(
            state = HomeState(),
            onAction = {}
        )
    }
}