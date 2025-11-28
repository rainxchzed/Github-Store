package zed.rainxch.githubstore.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.core.domain.getPlatform
import zed.rainxch.githubstore.feature.auth.data.DesktopTokenStore
import zed.rainxch.githubstore.feature.auth.data.TokenStore
import zed.rainxch.githubstore.feature.details.data.Downloader
import zed.rainxch.githubstore.feature.details.data.FileLocationsProvider
import zed.rainxch.githubstore.feature.details.data.Installer
import zed.rainxch.githubstore.feature.details.data.DesktopDownloader
import zed.rainxch.githubstore.feature.details.data.DesktopFileLocationsProvider
import zed.rainxch.githubstore.feature.details.data.DesktopInstaller

actual val platformModule: Module = module {
    single<Downloader> {
        DesktopDownloader(
            http = get(),
            files = get()
        )
    }

    single<Installer> {
        val platform = getPlatform()
        DesktopInstaller(
            platform = platform.type
        )
    }

    single<FileLocationsProvider> {
        val platform = getPlatform()
        DesktopFileLocationsProvider(platform = platform.type)
    }

    single<TokenStore> {
        DesktopTokenStore()
    }
}