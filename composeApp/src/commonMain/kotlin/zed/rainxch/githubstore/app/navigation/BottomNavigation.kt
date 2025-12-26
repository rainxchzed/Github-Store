package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import zed.rainxch.githubstore.app.navigation.locals.LocalBottomNavLiquidState
import zed.rainxch.githubstore.core.presentation.utils.isLiquidTopbarEnabled

@Composable
fun BottomNavigation(
    currentScreen: GithubStoreGraph,
    items: ImmutableList<BottomNavItem>,
    onNavigate: (GithubStoreGraph) -> Unit,
    modifier: Modifier = Modifier
) {
    val liquidState = LocalBottomNavLiquidState.current
    val isBottomNavVisible = remember(currentScreen) {
        BottomNavUtils.allowedScreens().any { it == currentScreen }
    }

    if (isBottomNavVisible) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = modifier
                .then(
                    if (isLiquidTopbarEnabled()) {
                        Modifier.liquid(liquidState) {
                            this.shape = CutCornerShape(0.dp)
                            this.frost = 8.dp
                            this.curve = .4f
                            this.refraction = .1f
                            this.dispersion = .2f
                        }
                    } else Modifier
                )
        ) {
            items.forEach { bottomNavItem ->
                NavigationBarItem(
                    selected = currentScreen == bottomNavItem.screen,
                    onClick = {
                        onNavigate(bottomNavItem.screen)
                    },
                    label = {
                        Text(
                            text = bottomNavItem.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(bottomNavItem.icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }
    }
}