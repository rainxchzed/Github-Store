package zed.rainxch.githubstore.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import zed.rainxch.githubstore.core.data.local.data_store.createDataStore
import zed.rainxch.githubstore.core.data.local.db.AppDatabase
import zed.rainxch.githubstore.core.data.local.db.initDatabase
import zed.rainxch.githubstore.core.data.services.AndroidApkInfoExtractor
import zed.rainxch.githubstore.core.data.services.AndroidDownloader
import zed.rainxch.githubstore.core.data.services.AndroidFileLocationsProvider
import zed.rainxch.githubstore.core.data.services.AndroidLocalizationManager
import zed.rainxch.githubstore.core.data.services.AndroidPackageMonitor
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.FileLocationsProvider
import zed.rainxch.githubstore.core.data.services.Installer
import zed.rainxch.githubstore.core.data.services.LocalizationManager
import zed.rainxch.githubstore.core.data.services.PackageMonitor
import zed.rainxch.githubstore.core.data.services.installer.AndroidInstaller
import zed.rainxch.githubstore.core.data.services.installer.shizuku.ShizukuInstaller
import zed.rainxch.githubstore.core.data.services.installer.shizuku.ShizukuManager
import zed.rainxch.githubstore.core.presentation.utils.AndroidAppLauncher
import zed.rainxch.githubstore.core.presentation.utils.AndroidBrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.AndroidClipboardHelper
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.core.presentation.utils.BrowserHelper
import zed.rainxch.githubstore.core.presentation.utils.ClipboardHelper
import zed.rainxch.githubstore.feature.auth.data.AndroidTokenStore
import zed.rainxch.githubstore.feature.auth.data.TokenStore

actual val platformModule: Module = module {
    single<Downloader> {
        AndroidDownloader(
            context = androidContext(),
            files = get()
        )
    }

    single<ShizukuInstaller> {
        ShizukuInstaller(
            context = androidContext(),
            shizukuManager = get()
        )
    }

    single<ShizukuManager> {
        ShizukuManager(
            context = get()
        )
    }


    single<Installer> {
        AndroidInstaller(
            context = androidContext(),
            apkInfoExtractor = AndroidApkInfoExtractor(androidContext()),
            shizukuManager = get(),
            shizukuInstaller = get()
        )
    }

    single<FileLocationsProvider> {
        AndroidFileLocationsProvider(
            context = androidContext()
        )
    }

    single<DataStore<Preferences>> {
        createDataStore(
            context = androidContext()
        )
    }

    single<BrowserHelper> {
        AndroidBrowserHelper(
            context = androidContext()
        )
    }

    single<ClipboardHelper> {
        AndroidClipboardHelper(
            context = androidContext()
        )
    }

    single<TokenStore> {
        AndroidTokenStore(
            dataStore = get()
        )
    }

    single<AppDatabase> {
        initDatabase(
            context = androidContext()
        )
    }

    single<PackageMonitor> {
        AndroidPackageMonitor(
            context = androidContext()
        )
    }

    single<LocalizationManager> {
        AndroidLocalizationManager()
    }

    single<AppLauncher> {
        AndroidAppLauncher(
            context = androidContext()
        )
    }
}