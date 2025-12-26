package zed.rainxch.githubstore.core.presentation.utils

import android.os.Build

actual fun isLiquidTopbarEnabled(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
