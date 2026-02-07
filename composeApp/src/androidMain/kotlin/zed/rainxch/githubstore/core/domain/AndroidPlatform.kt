package zed.rainxch.githubstore.core.domain

import zed.rainxch.core.domain.model.Platform

class AndroidPlatform : Platform {
    override val type: Platform
        get() = Platform.ANDROID

}

actual fun getPlatform(): Platform {
    return AndroidPlatform()
}