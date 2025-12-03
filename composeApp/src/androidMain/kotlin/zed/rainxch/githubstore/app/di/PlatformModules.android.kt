package zed.rainxch.githubstore.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.feature.auth.data.AndroidTokenStore
import zed.rainxch.githubstore.feature.auth.data.TokenStore
import zed.rainxch.githubstore.feature.details.data.AndroidDownloader
import zed.rainxch.githubstore.feature.details.data.AndroidFileLocationsProvider
import zed.rainxch.githubstore.feature.details.data.AndroidInstaller
import zed.rainxch.githubstore.feature.details.data.Downloader
import zed.rainxch.githubstore.feature.details.data.FileLocationsProvider
import zed.rainxch.githubstore.feature.details.data.Installer

actual val platformModule: Module = module {
    single<Downloader> {
        AndroidDownloader(
            context = get(),
            files = get()
        )
    }

    single<Installer> {
        AndroidInstaller(
            context = get(),
        )
    }

    single<FileLocationsProvider> {
        AndroidFileLocationsProvider(context = get())
    }

    single<TokenStore> {
        AndroidTokenStore()
    }
}