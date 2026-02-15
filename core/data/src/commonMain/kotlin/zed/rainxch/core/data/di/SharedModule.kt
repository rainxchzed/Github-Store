package zed.rainxch.core.data.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.data.data_source.impl.DefaultTokenStore
import zed.rainxch.core.data.local.db.AppDatabase
import zed.rainxch.core.data.local.db.dao.FavoriteRepoDao
import zed.rainxch.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.core.data.local.db.dao.StarredRepoDao
import zed.rainxch.core.data.local.db.dao.UpdateHistoryDao
import zed.rainxch.core.data.logging.KermitLogger
import zed.rainxch.core.data.network.createGitHubHttpClient
import zed.rainxch.core.data.repository.AuthenticationStateImpl
import zed.rainxch.core.data.repository.FavouritesRepositoryImpl
import zed.rainxch.core.data.repository.InstalledAppsRepositoryImpl
import zed.rainxch.core.data.repository.RateLimitRepositoryImpl
import zed.rainxch.core.data.repository.StarredRepositoryImpl
import zed.rainxch.core.data.repository.ThemesRepositoryImpl
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.repository.AuthenticationState
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.RateLimitRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.repository.ThemesRepository
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase

val coreModule = module {
    single {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    single<GitHubStoreLogger> {
        KermitLogger
    }

    single<Platform> {
        getPlatform()
    }

    single<AuthenticationState> {
        AuthenticationStateImpl(
            tokenStore = get()
        )
    }

    single<FavouritesRepository> {
        FavouritesRepositoryImpl(
            favoriteRepoDao = get(),
            installedAppsDao = get()
        )
    }

    single<InstalledAppsRepository> {
        InstalledAppsRepositoryImpl(
            database = get(),
            installedAppsDao = get(),
            historyDao = get(),
            installer = get(),
            downloader = get(),
            httpClient = get()
        )
    }

    single<StarredRepository> {
        StarredRepositoryImpl(
            installedAppsDao = get(),
            starredRepoDao = get(),
            platform = get(),
            httpClient = get()
        )
    }

    single<ThemesRepository> {
        ThemesRepositoryImpl(
            preferences = get()
        )
    }

    single<SyncInstalledAppsUseCase> {
        SyncInstalledAppsUseCase(
            packageMonitor = get(),
            installedAppsRepository = get(),
            platform = get(),
            logger = get()
        )
    }
}

val networkModule = module {
    single<HttpClient> {
        createGitHubHttpClient(
            tokenStore = get(),
            rateLimitRepository = get()
        )
    }

    single<TokenStore> {
        DefaultTokenStore(
            dataStore = get()
        )
    }

    single<RateLimitRepository> {
        RateLimitRepositoryImpl()
    }
}

val databaseModule = module {
    single<FavoriteRepoDao> {
        get<AppDatabase>().favoriteRepoDao
    }

    single<InstalledAppDao> {
        get<AppDatabase>().installedAppDao
    }

    single<StarredRepoDao> {
        get<AppDatabase>().starredReposDao
    }

    single<UpdateHistoryDao> {
        get<AppDatabase>().updateHistoryDao
    }
}