package zed.rainxch.githubstore.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import zed.rainxch.githubstore.MainViewModel
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.data.PackageMonitor
import zed.rainxch.githubstore.core.data.data_source.DefaultTokenDataSource
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.data.local.db.AppDatabase
import zed.rainxch.githubstore.core.data.repository.FavoritesRepositoryImpl
import zed.rainxch.githubstore.core.data.repository.InstalledAppsRepositoryImpl
import zed.rainxch.githubstore.core.data.repository.ThemesRepositoryImpl
import zed.rainxch.githubstore.core.domain.getPlatform
import zed.rainxch.githubstore.core.domain.repository.FavoritesRepository
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.ThemesRepository
import zed.rainxch.githubstore.network.buildAuthedGitHubHttpClient
import zed.rainxch.githubstore.feature.auth.data.repository.AuthRepositoryImpl
import zed.rainxch.githubstore.feature.auth.domain.*
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthRepository
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationViewModel
import zed.rainxch.githubstore.feature.details.data.repository.DetailsRepositoryImpl
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.details.presentation.DetailsViewModel
import zed.rainxch.githubstore.feature.details.data.Downloader
import zed.rainxch.githubstore.feature.details.data.Installer
import zed.rainxch.githubstore.feature.home.data.repository.HomeRepositoryImpl
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import zed.rainxch.githubstore.feature.home.presentation.HomeViewModel
import zed.rainxch.githubstore.feature.search.data.repository.SearchRepositoryImpl
import zed.rainxch.githubstore.feature.search.domain.repository.SearchRepository
import zed.rainxch.githubstore.feature.search.presentation.SearchViewModel
import zed.rainxch.githubstore.feature.settings.data.repository.SettingsRepositoryImpl
import zed.rainxch.githubstore.feature.settings.domain.repository.SettingsRepository
import zed.rainxch.githubstore.feature.settings.presentation.SettingsViewModel
import zed.rainxch.githubstore.network.RateLimitHandler

val coreModule: Module = module {
    single<TokenDataSource> {
        DefaultTokenDataSource(
            tokenStore = get()
        )
    }

    single { RateLimitHandler() }

    single {
        AppStateManager(
            rateLimitHandler = get(),
            tokenDataSource = get()
        )
    }

    single {
        buildAuthedGitHubHttpClient(
            tokenDataSource = get(),
            rateLimitHandler = get()
        )
    }

    single<ThemesRepository> {
        ThemesRepositoryImpl(
            preferences = get()
        )
    }

    viewModel {
        MainViewModel(
            tokenDataSource = get(),
            themesRepository = get(),
            appStateManager = get(),
            installedAppsRepository = get(),
            packageMonitor = get()
        )
    }

    single { get<AppDatabase>().installedAppDao }
    single { get<AppDatabase>().favoriteRepoDao }
    single { get<AppDatabase>().updateHistoryDao }

    single<FavoritesRepository> {
        FavoritesRepositoryImpl(
            dao = get(),
            installedAppsDao = get(),
            detailsRepository = get()
        )
    }

    single<CoroutineScope> {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    single<InstalledAppsRepository> {
        InstalledAppsRepositoryImpl(
            dao = get(),
            historyDao = get(),
            detailsRepository = get()
        )
    }
}

val authModule: Module = module {
    single<AuthRepository> { AuthRepositoryImpl(tokenDataSource = get()) }

    factory { StartDeviceFlowUseCase(get()) }
    factory { AwaitDeviceTokenUseCase(get()) }
    factory { ObserveAccessTokenUseCase(get()) }
    factory { IsAuthenticatedUseCase(get()) }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    viewModel {
        AuthenticationViewModel(
            startDeviceFlow = get(),
            awaitDeviceToken = get(),
            observeAccessToken = get(),
            browserHelper = get(),
            clipboardHelper = get(),
            scope = get(),
        )
    }
}

val homeModule: Module = module {
    single<HomeRepository> {
        HomeRepositoryImpl(
            githubNetworkClient = get(),
            platform = getPlatform(),
            appStateManager = get()
        )
    }

    viewModel {
        HomeViewModel(
            homeRepository = get(),
        )
    }
}

val searchModule: Module = module {
    single<SearchRepository> {
        SearchRepositoryImpl(
            githubNetworkClient = get(),
            appStateManager = get()
        )
    }

    viewModel {
        SearchViewModel(
            searchRepository = get()
        )
    }
}

val detailsModule: Module = module {
    single<DetailsRepository> {
        DetailsRepositoryImpl(
            github = get(),
            appStateManager = get()
        )
    }

    viewModel { params ->
        DetailsViewModel(
            repositoryId = params.get(),
            detailsRepository = get(),
            downloader = get<Downloader>(),
            installer = get<Installer>(),
            platform = getPlatform(),
            helper = get(),
            installedAppsRepository = get(),
            favoritesRepository = get(),
            packageMonitor = get<PackageMonitor>()
        )
    }
}

val settingsModule: Module = module {
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            tokenDataSource = get()
        )
    }

    viewModel {
        SettingsViewModel(
            browserHelper = get(),
            themesRepository = get(),
            settingsRepository = get()
        )
    }
}
