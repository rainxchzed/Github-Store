package zed.rainxch.feed.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.isAndroid
import zed.rainxch.core.domain.isDesktop
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.repository.FeedCategory
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.core.presentation.components.cards.KomiRepoCardFeed
import zed.rainxch.core.presentation.components.dividers.KomiHorizontalDivider
import zed.rainxch.core.presentation.components.overlays.KomiToastState
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.refresh.KomiPullToRefresh
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.MangaPersonality
import zed.rainxch.core.presentation.personality.usesDecor
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.toLabel
import zed.rainxch.core.presentation.layout.CardGridSpec
import zed.rainxch.core.presentation.layout.rememberGridColumns
import zed.rainxch.feed.presentation.components.FeedCategoryStrip
import zed.rainxch.feed.presentation.components.FeedPlatformBar
import zed.rainxch.feed.presentation.components.FeedPlatformPicker
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.feed_empty_reset
import zed.rainxch.githubstore.core.presentation.res.feed_empty_subtitle
import zed.rainxch.githubstore.core.presentation.res.feed_empty_title
import zed.rainxch.githubstore.core.presentation.res.feed_end_cap
import zed.rainxch.githubstore.core.presentation.res.feed_failed_to_load
import zed.rainxch.githubstore.core.presentation.res.feed_loading
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_subtitle
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_title
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_title_accent
import zed.rainxch.githubstore.core.presentation.res.feed_offline
import zed.rainxch.githubstore.core.presentation.res.home_retry

@Composable
fun FeedRoot(
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val listState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FeedEvent.OnMessage -> toastState.show(event.message)
            FeedEvent.OnScrollToTop -> coroutineScope.launch { listState.scrollToItem(0) }
        }
    }

    FeedScreen(
        state = state,
        toastState = toastState,
        listState = listState,
        onAction = { action ->
            when (action) {
                FeedAction.OnSearchClick -> onNavigateToSearch()
                FeedAction.OnProfileClick -> onNavigateToProfile()
                is FeedAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen(
    state: FeedState,
    toastState: KomiToastState,
    listState: LazyStaggeredGridState,
    onAction: (FeedAction) -> Unit,
) {
    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(reachedEnd, state.hasMore, state.isLoadingMore) {
        if (reachedEnd && state.hasMore && !state.isLoadingMore) {
            onAction(FeedAction.OnLoadMore)
        }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.feed_masthead_title),
                titleAccent = stringResource(Res.string.feed_masthead_title_accent),
                subtitle = if (LocalPersonality.current.usesDecor) stringResource(Res.string.feed_masthead_subtitle) else null,
            )
        },
        toastState = toastState,
    ) { innerPadding ->
        if (isAndroid()) {
            KomiPullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(FeedAction.OnRefresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                FeedContent(
                    listState = listState,
                    state = state,
                    onAction = onAction,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                FeedContent(
                    listState = listState,
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }

    if (state.isPlatformPickerVisible) {
        FeedPlatformPicker(
            selected = state.selectedPlatform,
            onSelect = { onAction(FeedAction.OnPlatformSelected(it)) },
            onDismiss = { onAction(FeedAction.OnPlatformPickerDismiss) },
        )
    }
}

@Composable
private fun BoxScope.FeedContent(
    listState: LazyStaggeredGridState,
    state: FeedState,
    onAction: (FeedAction) -> Unit,
) {
    val colors = LocalPersonality.current.colors
    val isManga = LocalPersonality.current is MangaPersonality
    val infoColumns = rememberGridColumns(CardGridSpec.InfoMaxCardWidth)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.TopCenter),
    ) {
        if (!isDesktop()) {
            Column(modifier = Modifier.fillMaxWidth().background(colors.background)) {
                FeedPlatformBar(
                    platform = state.selectedPlatform,
                    onOpenPicker = { onAction(FeedAction.OnPlatformPickerOpen) },
                )

                FeedCategoryStrip(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = { onAction(FeedAction.OnCategorySelected(it)) },
                )

                if (isManga) {
                    KomiHorizontalDivider(thickness = 3.dp, color = colors.outline)
                }
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(infoColumns),
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                state.isLoading && state.repos.isEmpty() -> {
                    item(key = "feed_loading", span = StaggeredGridItemSpan.FullLine) { FeedLoading() }
                }

                state.errorMessage != null && state.repos.isEmpty() -> {
                    item(key = "feed_error", span = StaggeredGridItemSpan.FullLine) {
                        FeedError(
                            message = state.errorMessage,
                            onRetry = { onAction(FeedAction.OnRetry) },
                        )
                    }
                }

                else -> {
                    if (state.isOffline) {
                        item(key = "feed_offline", span = StaggeredGridItemSpan.FullLine) {
                            KomiText(
                                text = stringResource(Res.string.feed_offline),
                                role = KomiTextRole.Label,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (state.repos.isEmpty()) {
                        item(key = "feed_empty", span = StaggeredGridItemSpan.FullLine) {
                            FeedEmpty(
                                category = state.selectedCategory,
                                platform = state.selectedPlatform,
                                onReset = { onAction(FeedAction.OnResetFilters) },
                            )
                        }
                    } else {
                        itemsIndexed(
                            state.repos,
                            key = { _, card -> "feed_${card.repository.id}" },
                        ) { _, card ->
                            DiscoveryRepoCard(
                                discoveryRepositoryUi = card,
                                onClick = { onAction(FeedAction.OnRepoClick(card.repository)) },
                                onShareClick = { onAction(FeedAction.OnShareClick(card.repository)) },
                                onHideClick = { onAction(FeedAction.OnHideRepository(card.repository)) },
                                onToggleSeen = {
                                    if (card.isSeen) {
                                        onAction(FeedAction.OnMarkAsUnseen(card.repository.id))
                                    } else {
                                        onAction(FeedAction.OnMarkAsSeen(card.repository))
                                    }
                                },
                                feed = KomiRepoCardFeed.Release,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (state.isLoadingMore) {
                            item(key = "feed_loading_more", span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    KomiCircularProgress(modifier = Modifier.size(28.dp))
                                }
                            }
                        } else if (!state.hasMore) {
                            item(key = "feed_end_cap", span = StaggeredGridItemSpan.FullLine) { FeedEndCap() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedEndCap() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KomiHorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = LocalPersonality.current.colors.outline.copy(alpha = 0.4f),
        )

        if (LocalPersonality.current.usesDecor) {
            KomiText(
                text = stringResource(Res.string.feed_end_cap),
                role = KomiTextRole.Label,
                color = LocalPersonality.current.colors.onSurfaceVariant,
            )
        }

        KomiHorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = LocalPersonality.current.colors.outline.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun FeedLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KomiCircularProgress()

            KomiText(
                text = stringResource(Res.string.feed_loading),
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.feed_failed_to_load),
                role = KomiTextRole.Title,
                textAlign = TextAlign.Center,
            )

            KomiText(
                text = message,
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            KomiButton(
                onClick = onRetry,
                label = stringResource(Res.string.home_retry),
                variant = KomiButtonVariant.Outline,
            )
        }
    }
}

@Composable
private fun FeedEmpty(
    category: FeedCategory,
    platform: DiscoveryPlatform,
    onReset: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.feed_empty_title),
                role = KomiTextRole.Title,
                textAlign = TextAlign.Center,
            )

            KomiText(
                text = stringResource(
                    Res.string.feed_empty_subtitle,
                    category.toLabel(),
                    platform.toLabel(),
                ),
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            KomiButton(
                onClick = onReset,
                label = stringResource(Res.string.feed_empty_reset),
                variant = KomiButtonVariant.Outline,
            )
        }
    }
}
