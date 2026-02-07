package zed.rainxch.githubstore.core.domain

import zed.rainxch.core.domain.model.Platform

class DesktopPlatform : Platform {
    override val type = when {
        System.getProperty("os.name").lowercase().contains("win") -> Platform.WINDOWS
        System.getProperty("os.name").lowercase().contains("mac") -> Platform.MACOS
        else -> Platform.LINUX
    }
}

actual fun getPlatform(): Platform {
    return DesktopPlatform()
}