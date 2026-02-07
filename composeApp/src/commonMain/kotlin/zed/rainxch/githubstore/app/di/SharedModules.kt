package zed.rainxch.githubstore.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import zed.rainxch.githubstore.MainViewModel
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.ThemesRepository
import zed.rainxch.githubstore.feature.apps.data.repository.AppsRepositoryImpl
import zed.rainxch.githubstore.feature.apps.domain.repository.AppsRepository
import zed.rainxch.githubstore.feature.apps.presentation.AppsViewModel
import zed.rainxch.core.data.network.buildAuthedGitHubHttpClient
import zed.rainxch.githubstore.feature.auth.data.repository.AuthenticationRepositoryImpl
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthenticationRepository
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationViewModel
import zed.rainxch.githubstore.feature.details.data.repository.DetailsRepositoryImpl
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.details.presentation.DetailsViewModel
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.githubstore.feature.developer_profile.data.repository.DeveloperProfileRepositoryImpl
import zed.rainxch.githubstore.feature.developer_profile.domain.repository.DeveloperProfileRepository
import zed.rainxch.githubstore.feature.favourites.presentation.FavouritesViewModel
import zed.rainxch.githubstore.feature.home.data.data_source.CachedTrendingDataSource
import zed.rainxch.githubstore.feature.home.data.repository.HomeRepositoryImpl
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import zed.rainxch.githubstore.feature.home.presentation.HomeViewModel
import zed.rainxch.githubstore.feature.developer_profile.presentation.DeveloperProfileViewModel
import zed.rainxch.githubstore.feature.search.data.repository.SearchRepositoryImpl
import zed.rainxch.githubstore.feature.search.domain.repository.SearchRepository
import zed.rainxch.githubstore.feature.search.presentation.SearchViewModel
import zed.rainxch.githubstore.feature.settings.data.repository.SettingsRepositoryImpl
import zed.rainxch.githubstore.feature.settings.domain.repository.SettingsRepository
import zed.rainxch.githubstore.feature.settings.presentation.SettingsViewModel
import zed.rainxch.githubstore.feature.starred_repos.presentation.StarredReposViewModel
import zed.rainxch.core.data.network.RateLimitHandler

val coreModule: Module = module {
    // Rate Limiting
    single { RateLimitHandler() }

    // App State Management
    single {
        AppStateManager(
            rateLimitHandler = get(),
            tokenDataSource = get()
        )
    }

    // Platform
    single { getPlatform() }

    // HTTP Client
    single {
        buildAuthedGitHubHttpClient(
            tokenStore = get(),
            rateLimitHandler = get()
        )
    }

    // Theme Management
    single<ThemesRepository> {
        ThemesRepositoryImpl(
            preferences = get()
        )
    }

    single {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    // Database DAOs (kept for repositories that need them)
    single { get<zed.rainxch.core.data.local.db.AppDatabase>().installedAppDao }
    single { get<zed.rainxch.core.data.local.db.AppDatabase>().favoriteRepoDao }
    single { get<zed.rainxch.core.data.local.db.AppDatabase>().updateHistoryDao }
    single { get<zed.rainxch.core.data.local.db.AppDatabase>().starredReposDao }

    single<SyncInstalledAppsUseCase> {
        SyncInstalledAppsUseCase(
            packageMonitor = get(),
            installedAppsRepository = get(),
            platform = get()
        )
    }

    // Repositories
    single<FavouritesRepository> {
        _root_ide_package_.zed.rainxch.core.data.repository.FavouritesRepositoryImpl(
            dao = get(),
            installedAppsDao = get(),
            detailsRepository = get()
        )
    }

    single<InstalledAppsRepository> {
        _root_ide_package_.zed.rainxch.core.data.repository.InstalledAppsRepositoryImpl(
            database = get(),
            dao = get(),
            historyDao = get(),
            detailsRepository = get(),
            installer = get(),
            downloader = get()
        )
    }

    // ViewModels
    viewModel {
        MainViewModel(
            tokenDataSource = get(),
            themesRepository = get(),
            appStateManager = get(),
            installedAppsRepository = get(),
            packageMonitor = get(),
            platform = get()
        )
    }
}

val authModule: Module = module {
    // Repository
    single<AuthenticationRepository> {
        AuthenticationRepositoryImpl(tokenDataSource = get())
    }

    // ViewModel
    viewModel {
        AuthenticationViewModel(
            authenticationRepository = get(),
            browserHelper = get(),
            clipboardHelper = get(),
            scope = get()
        )
    }
}

val homeModule: Module = module {
    // Repository
    single<HomeRepository> {
        HomeRepositoryImpl(
            githubNetworkClient = get(),
            platform = get(),
            appStateManager = get(),
            cachedDataSource = get()
        )
    }

    single<CachedTrendingDataSource> {
        CachedTrendingDataSource(
            platform = get()
        )
    }

    // ViewModel
    viewModel {
        HomeViewModel(
            homeRepository = get(),
            installedAppsRepository = get(),
            platform = get(),
            syncInstalledAppsUseCase = get(),
            favouritesRepository = get(),
            starredRepository = get()
        )
    }
}

val searchModule: Module = module {
    // Repository
    single<SearchRepository> {
        SearchRepositoryImpl(
            githubNetworkClient = get(),
            appStateManager = get()
        )
    }


    // ViewModel
    viewModel {
        SearchViewModel(
            searchRepository = get(),
            installedAppsRepository = get(),
            syncInstalledAppsUseCase = get(),
            favouritesRepository = get(),
            starredRepository = get()
        )
    }
}
val favouritesModule: Module = module {
    // ViewModel
    viewModel {
        FavouritesViewModel(
            favouritesRepository = get()
        )
    }
}

val detailsModule: Module = module {
    // Repository
    single<DetailsRepository> {
        DetailsRepositoryImpl(
            github = get(),
            appStateManager = get(),
            localizationManager = get()
        )
    }

    // ViewModel
    viewModel { params ->
        DetailsViewModel(
            repositoryId = params.get(),
            detailsRepository = get(),
            downloader = get<Downloader>(),
            installer = get<zed.rainxch.core.data.services.Installer>(),
            platform = get(),
            helper = get(),
            installedAppsRepository = get(),
            favouritesRepository = get(),
            packageMonitor = get<zed.rainxch.core.data.services.PackageMonitor>(),
            syncInstalledAppsUseCase = get(),
            starredRepository = get()
        )
    }
}
val repoAuthorModule: Module = module {
    // Repository
    single<DeveloperProfileRepository> {
        DeveloperProfileRepositoryImpl(
            httpClient = get(),
            platform = get(),
            installedAppsDao = get(),
            favouritesRepository = get()
        )
    }

    // ViewModel
    viewModel { params ->
        DeveloperProfileViewModel(
            repository = get(),
            favouritesRepository = get(),
            username = params.get(),
        )
    }
}

val settingsModule: Module = module {
    // Repository
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            tokenDataSource = get()
        )
    }

    // ViewModel
    viewModel {
        SettingsViewModel(
            browserHelper = get(),
            themesRepository = get(),
            settingsRepository = get()
        )
    }
}
val starredReposModule: Module = module {
    // Repository
    single<StarredRepository> {
        _root_ide_package_.zed.rainxch.core.data.repository.StarredRepositoryImpl(
            httpClient = get(),
            dao = get(),
            installedAppsDao = get(),
            platform = get()
        )
    }

    // ViewModel
    viewModel {
        StarredReposViewModel(
            starredRepository = get(),
            favouritesRepository = get(),
            tokenDataSource = get()
        )
    }
}

val appsModule: Module = module {
    // Repository
    single<AppsRepository> {
        AppsRepositoryImpl(
            appLauncher = get(),
            appsRepository = get()
        )
    }

    // ViewModel
    viewModel {
        AppsViewModel(
            appsRepository = get(),
            installedAppsRepository = get(),
            installer = get(),
            downloader = get(),
            packageMonitor = get(),
            detailsRepository = get(),
            platform = get(),
            syncInstalledAppsUseCase = get()
        )
    }
}