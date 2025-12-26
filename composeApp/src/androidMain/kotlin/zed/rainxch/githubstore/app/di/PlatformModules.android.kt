package zed.rainxch.githubstore.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.core.data.services.AndroidApkInfoExtractor
import zed.rainxch.githubstore.core.data.services.AndroidPackageMonitor
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.data.local.data_store.createDataStore
import zed.rainxch.githubstore.core.data.local.db.AppDatabase
import zed.rainxch.githubstore.core.data.local.db.initDatabase
import zed.rainxch.githubstore.core.presentation.utils.AndroidAppLauncher
import zed.rainxch.githubstore.core.presentation.utils.AndroidBrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.AndroidClipboardHelper
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.ClipboardHelper
import zed.rainxch.githubstore.feature.auth.data.AndroidTokenStore
import zed.rainxch.githubstore.feature.auth.data.TokenStore
import zed.rainxch.githubstore.core.data.services.AndroidDownloader
import zed.rainxch.githubstore.core.data.services.AndroidFileLocationsProvider
import zed.rainxch.githubstore.core.data.services.AndroidInstaller
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.FileLocationsProvider
import zed.rainxch.githubstore.core.data.services.Installer

actual val platformModule: Module = module {
    single<Downloader> {
        AndroidDownloader(
            context = androidContext(),
            files = get()
        )
    }

    single<Installer> {
        AndroidInstaller(
            context = androidContext(),
            apkInfoExtractor = AndroidApkInfoExtractor(androidContext())
        )
    }

    single<FileLocationsProvider> {
        AndroidFileLocationsProvider(androidContext())
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

    single<TokenStore> {
        AndroidTokenStore(
            dataStore = get()
        )
    }

    single<AppDatabase> {
        initDatabase(androidContext())
    }

    single<PackageMonitor> {
        AndroidPackageMonitor(androidContext())
    }

    single<AppLauncher> {
        AndroidAppLauncher(androidContext())
    }
}