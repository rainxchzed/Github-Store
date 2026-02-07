package zed.rainxch.githubstore.feature.details.presentation.utils

import android.os.Build

actual fun isLiquidFrostAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
