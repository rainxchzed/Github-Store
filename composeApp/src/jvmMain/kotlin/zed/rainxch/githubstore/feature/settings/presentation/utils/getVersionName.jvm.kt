package zed.rainxch.githubstore.feature.settings.presentation.utils

import zed.rainxch.githubstore.BuildConfig

actual fun getVersionName(): String {
    val fromSys = System.getProperty("VERSION_NAME")?.trim().orEmpty()
    if (fromSys.isNotEmpty()) return fromSys

    val fromEnv = System.getenv("VERSION_NAME")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return BuildConfig.VERSION_NAME
}