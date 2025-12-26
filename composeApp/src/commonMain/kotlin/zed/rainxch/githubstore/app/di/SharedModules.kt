package zed.rainxch.githubstore.app.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zed.rainxch.githubstore.MainViewModel
import zed.rainxch.githubstore.app.app_state.AppStateManager
import zed.rainxch.githubstore.core.data.data_source.DefaultTokenDataSource
import zed.rainxch.githubstore.core.data.data_source.OAuthTokenRefresher
import zed.rainxch.githubstore.core.data.data_source.TokenDataSource
import zed.rainxch.githubstore.core.data.data_source.TokenRefresher
import zed.rainxch.githubstore.core.data.local.db.AppDatabase
import zed.rainxch.githubstore.core.data.repository.FavoritesRepositoryImpl
import zed.rainxch.githubstore.core.data.repository.InstalledAppsRepositoryImpl
import zed.rainxch.githubstore.core.data.repository.ThemesRepositoryImpl
import zed.rainxch.githubstore.core.domain.Platform
import zed.rainxch.githubstore.core.domain.getPlatform
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.repository.FavoritesRepository
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.domain.repository.ThemesRepository
import zed.rainxch.githubstore.feature.apps.data.repository.AppsRepositoryImpl
import zed.rainxch.githubstore.feature.apps.domain.repository.AppsRepository
import zed.rainxch.githubstore.feature.apps.presentation.AppsViewModel
import zed.rainxch.githubstore.feature.auth.data.repository.AuthenticationRepositoryImpl
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthenticationRepository
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationViewModel
import zed.rainxch.githubstore.feature.details.data.repository.DetailsRepositoryImpl
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import zed.rainxch.githubstore.feature.details.presentation.DetailsViewModel
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
import zed.rainxch.githubstore.network.buildAuthedGitHubHttpClient
import zed.rainxch.githubstore.network.buildAuthedGitLabHttpClient

val coreModule: Module = module {
    // Platform
    single<Platform> {
        getPlatform()
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
    single { get<AppDatabase>().installedAppDao }
    single { get<AppDatabase>().favoriteRepoDao }
    single { get<AppDatabase>().updateHistoryDao }

    // Repositories
    single<FavoritesRepository> {
        FavoritesRepositoryImpl(
            dao = get(),
            installedAppsDao = get(),
            detailsRepository = get(named("github"))
        )
    }

    single<InstalledAppsRepository> {
        InstalledAppsRepositoryImpl(
            dao = get(),
            historyDao = get(),
            detailsRepository = get(named("github")),
            installer = get(),
            downloader = get()
        )
    }

    // ViewModels
    viewModel {
        MainViewModel(
            themesRepository = get(),
            appStateManager = get(),
            installedAppsRepository = get(),
            packageMonitor = get(),
            gitlabTokenDataSource = get(TokenDataStoreQualifiers.GitLab),
            githubTokenDataSource = get(TokenDataStoreQualifiers.Github),
            platform = get()
        )
    }
}

val authModule: Module = module {
    // Repository
    single<AuthenticationRepository>(named("github")) {
        AuthenticationRepositoryImpl(
            tokenDataSource = get(TokenDataStoreQualifiers.Github),
            apiPlatform = ApiPlatform.Github
        )
    }

    single<AuthenticationRepository>(named("gitlab")) {
        AuthenticationRepositoryImpl(
            tokenDataSource = get(TokenDataStoreQualifiers.GitLab),
            apiPlatform = ApiPlatform.GitLab
        )
    }


    // ViewModel
    viewModel { params ->
        val platform = params.get<ApiPlatform>()

        AuthenticationViewModel(
            apiPlatform = platform,
            authenticationRepository = get(
                if (platform == ApiPlatform.Github)
                    named("github")
                else
                    named("gitlab")
            ),
            browserHelper = get(),
            clipboardHelper = get(),
            scope = get()
        )
    }
}

val homeModule: Module = module {
    // Repository
    single<HomeRepository>(named("github")) {
        HomeRepositoryImpl(
            platform = get(),
            appStateManager = get(),
            httpClient = get(NetworkQualifiers.Github),
            apiPlatform = ApiPlatform.Github
        )
    }

    single<HomeRepository>(named("gitlab")) {
        HomeRepositoryImpl(
            platform = get(),
            appStateManager = get(),
            httpClient = get(NetworkQualifiers.GitLab),
            apiPlatform = ApiPlatform.GitLab
        )
    }

    // ViewModel
    viewModel { params ->
        HomeViewModel(
            homeRepository = get(
                qualifier = if (params.get<ApiPlatform>() == ApiPlatform.Github) {
                    named("github")
                } else {
                    named("gitlab")
                }
            ),
            installedAppsRepository = get(),
            platform = get()
        )
    }
}

val searchModule: Module = module {
    // Repository
    single<SearchRepository>(named("github")) {
        SearchRepositoryImpl(
            githubNetworkClient = get(NetworkQualifiers.Github),
            appStateManager = get(),
            apiPlatform = ApiPlatform.Github
        )
    }
    single<SearchRepository>(named("gitlab")) {
        SearchRepositoryImpl(
            githubNetworkClient = get(NetworkQualifiers.GitLab),
            appStateManager = get(),
            apiPlatform = ApiPlatform.GitLab
        )
    }


    // ViewModel
    viewModel {
        SearchViewModel(
            searchRepository = get(named("github")),
            installedAppsRepository = get()
        )
    }
}

val detailsModule: Module = module {
    // Repository
    single<DetailsRepository>(named("github")) {
        DetailsRepositoryImpl(
            httpClient = get(NetworkQualifiers.Github),
            appStateManager = get(),
            apiPlatform = ApiPlatform.Github
        )
    }

    single<DetailsRepository>(named("gitlab")) {
        DetailsRepositoryImpl(
            httpClient = get(NetworkQualifiers.GitLab),
            appStateManager = get(),
            apiPlatform = ApiPlatform.GitLab
        )
    }

    // ViewModel
    viewModel { params ->
        val platform = params.get<ApiPlatform>()

        DetailsViewModel(
            repositoryId = params.get(),
            detailsRepository = get(
                qualifier = if (platform == ApiPlatform.Github)
                    named("github")
                else
                    named("gitlab")
            ),
            downloader = get(),
            installer = get(),
            platform = get(),
            helper = get(),
            installedAppsRepository = get(),
            favoritesRepository = get(),
            packageMonitor = get()
        )
    }
}

val settingsModule: Module = module {
    // Repository
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            tokenDataSource = get(TokenDataStoreQualifiers.Github),
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

val appsModule: Module = module {
    // Repository
    single<AppsRepository> {
        AppsRepositoryImpl(
            appLauncher = get(),
            installedAppsRepository = get()
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
            detailsRepository = get(named("github"))
        )
    }
}

val networkModule = module {
    single(NetworkQualifiers.Github) {
        buildAuthedGitHubHttpClient(
            tokenDataSource = get<TokenDataSource>(TokenDataStoreQualifiers.Github),
            rateLimitHandler = get()
        )
    }

    single(NetworkQualifiers.GitLab) {
        buildAuthedGitLabHttpClient(
            tokenDataSource = get<TokenDataSource>(TokenDataStoreQualifiers.GitLab),
            rateLimitHandler = get()
        )
    }
}

val tokenModule = module {
    single<TokenRefresher> {
        OAuthTokenRefresher(httpClient = HttpClient())
    }

    // Token Management
    single<TokenDataSource>(TokenDataStoreQualifiers.Github) {
        DefaultTokenDataSource(
            tokenStore = get(),
            apiPlatform = ApiPlatform.Github,
            tokenRefresher = get(),
            scope = get()
        )
    }

    single<TokenDataSource>(TokenDataStoreQualifiers.GitLab) {
        DefaultTokenDataSource(
            tokenStore = get(),
            apiPlatform = ApiPlatform.GitLab,
            tokenRefresher = get(),
            scope = get()
        )
    }

    // Rate Limiting
    single<RateLimitHandler> {
        RateLimitHandler()
    }

    // App State Management
    single {
        AppStateManager(
            rateLimitHandler = get(),
            githubTokenDataSource = get(TokenDataStoreQualifiers.Github),
            gitlabTokenDataSource = get(TokenDataStoreQualifiers.GitLab)
        )
    }
}

object NetworkQualifiers {
    val Github = named("github")
    val GitLab = named("gitlab")
}

object TokenDataStoreQualifiers {
    val Github = named("github")
    val GitLab = named("gitlab")
}