@file:Suppress("DEPRECATION")

package zed.rainxch.core.presentation.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun ScrollbarContainer(
    listState: LazyListState,
    enabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }
    Box(modifier = modifier.padding(start = 8.dp)) {
        val scrollbarStyle =
            LocalScrollbarStyle.current.copy(
                shape = RoundedCornerShape(32.dp),
                unhoverColor = MaterialTheme.colorScheme.onSurface,
                hoverColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        content()
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier =
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd),
            style = scrollbarStyle,
        )
    }
}

@Composable
actual fun ScrollbarContainer(
    gridState: LazyStaggeredGridState,
    enabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }

        return
    }
    Box(modifier = modifier.padding(start = 8.dp)) {
        val scrollbarStyle =
            LocalScrollbarStyle.current.copy(
                shape = RoundedCornerShape(32.dp),
                unhoverColor = MaterialTheme.colorScheme.onSurface,
                hoverColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        content()
        val adapter = remember(gridState) { StaggeredGridScrollbarAdapter(gridState) }
        VerticalScrollbar(
            adapter = adapter,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd),
            style = scrollbarStyle,
        )
    }
}

@Composable
actual fun ScrollbarContainer(
    gridState: LazyGridState,
    enabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }

        return
    }
    Box(modifier = modifier.padding(start = 8.dp)) {
        val scrollbarStyle =
            LocalScrollbarStyle.current.copy(
                shape = RoundedCornerShape(32.dp),
                unhoverColor = MaterialTheme.colorScheme.onSurface,
                hoverColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        content()
        val adapter = remember(gridState) { GridScrollbarAdapter(gridState) }
        VerticalScrollbar(
            adapter = adapter,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd),
            style = scrollbarStyle,
        )
    }
}

private class StaggeredGridScrollbarAdapter(
    private val gridState: LazyStaggeredGridState,
) : ScrollbarAdapter {
    override val scrollOffset: Float
        get() {
            val layoutInfo = gridState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
            val fraction = firstVisible.index.toFloat() / maxOf(layoutInfo.totalItemsCount, 1)
            return fraction * estimatedContentSize() - firstVisible.offset.y.toFloat()
        }

    override fun maxScrollOffset(containerSize: Int): Float = (estimatedContentSize() - containerSize).coerceAtLeast(0f)

    override suspend fun scrollTo(
        containerSize: Int,
        scrollOffset: Float,
    ) {
        val totalContent = estimatedContentSize()
        val layoutInfo = gridState.layoutInfo
        val maxOffset = maxScrollOffset(containerSize)
        if (layoutInfo.totalItemsCount == 0 || totalContent <= 0f || maxOffset <= 0f) return
        val fraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)

        val targetIndex =
            (fraction * (layoutInfo.totalItemsCount - 1))
                .toInt()
                .coerceIn(0, maxOf(layoutInfo.totalItemsCount - 1, 0))
        gridState.scrollToItem(targetIndex)
    }

    private fun estimatedContentSize(): Float {
        val layoutInfo = gridState.layoutInfo
        if (layoutInfo.totalItemsCount == 0) return 0f
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f
        val avgHeight = visibleItems.map { it.size.height }.average().toFloat()
        val laneCount =
            maxOf(
                visibleItems.maxOf { it.lane + 1 },
                1,
            )
        val rows = (layoutInfo.totalItemsCount + laneCount - 1) / laneCount
        return rows * avgHeight + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
    }
}

private class GridScrollbarAdapter(
    private val gridState: LazyGridState,
) : ScrollbarAdapter {
    override val scrollOffset: Float
        get() {
            val layoutInfo = gridState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
            val fraction = firstVisible.index.toFloat() / maxOf(layoutInfo.totalItemsCount, 1)
            return fraction * estimatedContentSize() - firstVisible.offset.y.toFloat()
        }

    override fun maxScrollOffset(containerSize: Int): Float = (estimatedContentSize() - containerSize).coerceAtLeast(0f)

    override suspend fun scrollTo(
        containerSize: Int,
        scrollOffset: Float,
    ) {
        val totalContent = estimatedContentSize()
        val layoutInfo = gridState.layoutInfo
        val maxOffset = maxScrollOffset(containerSize)
        if (layoutInfo.totalItemsCount == 0 || totalContent <= 0f || maxOffset <= 0f) return
        val fraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)

        val targetIndex =
            (fraction * (layoutInfo.totalItemsCount - 1))
                .toInt()
                .coerceIn(0, maxOf(layoutInfo.totalItemsCount - 1, 0))
        gridState.scrollToItem(targetIndex)
    }

    private fun estimatedContentSize(): Float {
        val layoutInfo = gridState.layoutInfo
        if (layoutInfo.totalItemsCount == 0) return 0f
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f
        val avgHeight = visibleItems.map { it.size.height }.average().toFloat()
        val laneCount =
            maxOf(
                visibleItems.maxOf { it.column + 1 },
                1,
            )
        val rows = (layoutInfo.totalItemsCount + laneCount - 1) / laneCount
        return rows * avgHeight + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
    }
}
