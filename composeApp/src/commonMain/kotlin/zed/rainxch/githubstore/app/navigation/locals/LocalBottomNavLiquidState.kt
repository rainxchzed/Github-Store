package zed.rainxch.githubstore.app.navigation.locals

import androidx.compose.runtime.compositionLocalOf
import io.github.fletchmckee.liquid.LiquidState

val LocalBottomNavLiquidState = compositionLocalOf<LiquidState> {
    error("State not provided!")
}