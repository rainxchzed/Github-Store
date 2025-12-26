package zed.rainxch.githubstore.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.core.data.services.DesktopApkInfoExtractor
import zed.rainxch.githubstore.core.data.services.DesktopPackageMonitor
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.data.local.data_store.createDataStore
import zed.rainxch.githubstore.core.data.local.db.AppDatabase
import zed.rainxch.githubstore.core.data.local.db.initDatabase
import zed.rainxch.githubstore.core.domain.getPlatform
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.ClipboardHelper
import zed.rainxch.githubstore.core.presentation.utils.DesktopAppLauncher
import zed.rainxch.githubstore.core.presentation.utils.JvmBrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.JvmClipboardHelper
import zed.rainxch.githubstore.feature.auth.data.DesktopTokenStore
import zed.rainxch.githubstore.feature.auth.data.TokenStore
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.FileLocationsProvider
import zed.rainxch.githubstore.core.data.services.Installer
import zed.rainxch.githubstore.core.data.services.DesktopDownloader
import zed.rainxch.githubstore.core.data.services.DesktopFileLocationsProvider
import zed.rainxch.githubstore.core.data.services.DesktopInstaller

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
            platform = platform.type,
            apkInfoExtractor = DesktopApkInfoExtractor()
        )
    }

    single<FileLocationsProvider> {
        val platform = getPlatform()
        DesktopFileLocationsProvider(
            platform = platform.type
        )
    }

    single<DataStore<Preferences>> {
        createDataStore()
    }

    single<ClipboardHelper> {
        JvmClipboardHelper()
    }

    single<BrowserHelper> {
        JvmBrowserHelper()
    }

    single<TokenStore> {
        DesktopTokenStore()
    }

    single<AppDatabase> {
        initDatabase()
    }

    single<PackageMonitor> {
        DesktopPackageMonitor()
    }

    single<AppLauncher> {
        DesktopAppLauncher(get())
    }
}