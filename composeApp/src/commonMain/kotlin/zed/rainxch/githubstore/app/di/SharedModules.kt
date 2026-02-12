package zed.rainxch.githubstore.app.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.githubstore.MainViewModel

val mainModule: Module = module {
    viewModel {
        MainViewModel(
            themesRepository = get(),
            installedAppsRepository = get(),
            rateLimitRepository = get(),
            syncUseCase = get(),
            authenticationState = get()
        )
    }
}

//val authModule: Module = module {
//    // Repository
//    single<AuthenticationRepository> {
//        AuthenticationRepositoryImpl(tokenDataSource = get())
//    }
//
//    // ViewModel
//    viewModel {
//        AuthenticationViewModel(
//            authenticationRepository = get(),
//            browserHelper = get(),
//            clipboardHelper = get(),
//            scope = get()
//        )
//    }
//}

//val homeModule: Module = module {
//    // Repository
//    single<HomeRepository> {
//        HomeRepositoryImpl(
//            githubNetworkClient = get(),
//            platform = get(),
//            appStateManager = get(),
//            cachedDataSource = get()
//        )
//    }
//
//    single<CachedTrendingDataSource> {
//        CachedTrendingDataSource(
//            platform = get()
//        )
//    }
//
//    // ViewModel
//    viewModel {
//        HomeViewModel(
//            homeRepository = get(),
//            installedAppsRepository = get(),
//            platform = get(),
//            syncInstalledAppsUseCase = get(),
//            favouritesRepository = get(),
//            starredRepository = get()
//        )
//    }
//}
//
//val searchModule: Module = module {
//    // Repository
//    single<SearchRepository> {
//        SearchRepositoryImpl(
//            githubNetworkClient = get(),
//            appStateManager = get()
//        )
//    }
//
//
//    // ViewModel
//    viewModel {
//        SearchViewModel(
//            searchRepository = get(),
//            installedAppsRepository = get(),
//            syncInstalledAppsUseCase = get(),
//            favouritesRepository = get(),
//            starredRepository = get()
//        )
//    }
//}
//val favouritesModule: Module = module {
//    // ViewModel
//    viewModel {
//        FavouritesViewModel(
//            favouritesRepository = get()
//        )
//    }
//}
//
//val detailsModule: Module = module {
//    // Repository
//    single<DetailsRepository> {
//        DetailsRepositoryImpl(
//            github = get(),
//            appStateManager = get(),
//            localizationManager = get()
//        )
//    }
//
//    // ViewModel
//    viewModel { params ->
//        DetailsViewModel(
//            repositoryId = params.get(),
//            detailsRepository = get(),
//            downloader = get<Downloader>(),
//            installer = get<Installer>(),
//            platform = get(),
//            helper = get(),
//            installedAppsRepository = get(),
//            favouritesRepository = get(),
//            packageMonitor = get<zed.rainxch.core.data.services.PackageMonitor>(),
//            syncInstalledAppsUseCase = get(),
//            starredRepository = get()
//        )
//    }
//}
//val repoAuthorModule: Module = module {
//    // Repository
//    single<DeveloperProfileRepository> {
//        DeveloperProfileRepositoryImpl(
//            httpClient = get(),
//            platform = get(),
//            installedAppsDao = get(),
//            favouritesRepository = get()
//        )
//    }
//
//    // ViewModel
//    viewModel { params ->
//        DeveloperProfileViewModel(
//            repository = get(),
//            favouritesRepository = get(),
//            username = params.get(),
//        )
//    }
//}
//
//val settingsModule: Module = module {
//    // Repository
//    single<SettingsRepository> {
//        SettingsRepositoryImpl(
//            tokenDataSource = get()
//        )
//    }
//
//    // ViewModel
//    viewModel {
//        SettingsViewModel(
//            browserHelper = get(),
//            themesRepository = get(),
//            settingsRepository = get()
//        )
//    }
//}
//val starredReposModule: Module = module {
//    // Repository
//    single<StarredRepository> {
//        _root_ide_package_.zed.rainxch.core.data.repository.StarredRepositoryImpl(
//            httpClient = get(),
//            starredRepoDao = get(),
//            installedAppsDao = get(),
//            platform = get()
//        )
//    }
//
//    // ViewModel
//    viewModel {
//        StarredReposViewModel(
//            starredRepository = get(),
//            favouritesRepository = get(),
//            tokenDataSource = get()
//        )
//    }
//}
//
//val appsModule: Module = module {
//    // Repository
//    single<AppsRepository> {
//        AppsRepositoryImpl(
//            appLauncher = get(),
//            appsRepository = get()
//        )
//    }
//
//    // ViewModel
//    viewModel {
//        AppsViewModel(
//            appsRepository = get(),
//            installedAppsRepository = get(),
//            installer = get(),
//            downloader = get(),
//            packageMonitor = get(),
//            detailsRepository = get(),
//            platform = get(),
//            syncInstalledAppsUseCase = get()
//        )
//    }
//}