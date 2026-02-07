package zed.rainxch.githubstore.app.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.core.data.services.AndroidApkInfoExtractor
import zed.rainxch.githubstore.core.data.services.AndroidLocalizationManager
import zed.rainxch.githubstore.core.data.services.AndroidPackageMonitor
import zed.rainxch.core.data.local.data_store.createDataStore
import zed.rainxch.githubstore.core.data.local.db.initDatabase
import zed.rainxch.githubstore.core.presentation.utils.AndroidAppLauncher
import zed.rainxch.githubstore.core.presentation.utils.AndroidBrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.AndroidClipboardHelper
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.ClipboardHelper
import zed.rainxch.core.data.data_source.impl.DefaultTokenStore
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.githubstore.core.data.services.AndroidDownloader
import zed.rainxch.githubstore.core.data.services.AndroidFileLocationsProvider
import zed.rainxch.githubstore.core.data.services.AndroidInstaller
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.system.PackageMonitor

actual val platformModule: Module = module {
    single<Downloader> {
        AndroidDownloader(
            context = get(),
            files = get()
        )
    }

    single<zed.rainxch.core.data.services.Installer> {
        AndroidInstaller(
            context = get(),
            apkInfoExtractor = AndroidApkInfoExtractor(androidContext())
        )
    }

    single<zed.rainxch.core.data.services.FileLocationsProvider> {
        AndroidFileLocationsProvider(context = get())
    }

    single<DataStore<Preferences>> {
        createDataStore(androidContext())
    }

    single<BrowserHelper> {
        AndroidBrowserHelper(androidContext())
    }

    single<ClipboardHelper> {
        AndroidClipboardHelper(androidContext())
    }

    single<zed.rainxch.core.data.local.db.AppDatabase> {
        initDatabase(androidContext())
    }

    single<PackageMonitor> {
        AndroidPackageMonitor(androidContext())
    }

    single<zed.rainxch.core.data.services.LocalizationManager> {
        AndroidLocalizationManager()
    }

    single<AppLauncher> {
        AndroidAppLauncher(androidContext())
    }
}