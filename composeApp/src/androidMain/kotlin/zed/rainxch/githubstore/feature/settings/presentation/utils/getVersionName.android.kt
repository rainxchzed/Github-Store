package zed.rainxch.githubstore.feature.settings.presentation.utils

import zed.rainxch.githubstore.BuildConfig

actual fun getVersionName(): String {
    return BuildConfig.VERSION_NAME
}