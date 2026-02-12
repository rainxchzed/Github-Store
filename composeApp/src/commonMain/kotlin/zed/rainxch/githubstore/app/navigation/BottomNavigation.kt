package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.liquid
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.presentation.locals.LocalBottomNavigationLiquid
import zed.rainxch.details.presentation.utils.isLiquidFrostAvailable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavigation(
    currentScreen: GithubStoreGraph,
    onNavigate: (GithubStoreGraph) -> Unit,
    modifier: Modifier = Modifier
) {
    val liquidState = LocalBottomNavigationLiquid.current
    if (currentScreen in BottomNavigationUtils.allowedScreens()) {
        Row(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .then(
                    if (isLiquidFrostAvailable()) {
                        Modifier.liquid(liquidState) {
                            this.shape = CircleShape
                            this.frost = 16.dp
                            this.curve = .4f
                            this.refraction = .1f
                            this.dispersion = .2f
                            this.saturation = .5f
                        }
                    } else Modifier
                )
                .pointerInput(Unit) { },
        ) {
            BottomNavigationUtils
                .items()
                .filterNot {
                    getPlatform() != Platform.ANDROID &&
                            it.screen == GithubStoreGraph.AppsScreen
                }
                .forEach { item ->
                    IconButton(
                        onClick = {
                            onNavigate(item.screen)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (item.screen == currentScreen) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else Color.Transparent,
                            contentColor = if (item.screen == currentScreen) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else MaterialTheme.colorScheme.onSurface,
                        ),
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = item.iconRes,
                            contentDescription = stringResource(item.titleRes),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
        }
    }
}