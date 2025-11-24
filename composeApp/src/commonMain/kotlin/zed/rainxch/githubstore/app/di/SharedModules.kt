package zed.rainxch.githubstore.app.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import zed.rainxch.githubstore.MainViewModel
import zed.rainxch.githubstore.core.data.DefaultTokenDataSource
import zed.rainxch.githubstore.core.data.TokenDataSource
import zed.rainxch.githubstore.network.buildAuthedGitHubHttpClient
import zed.rainxch.githubstore.feature.auth.data.repository.AuthRepositoryImpl
import zed.rainxch.githubstore.feature.auth.domain.*
import zed.rainxch.githubstore.feature.auth.domain.repository.AuthRepository
import zed.rainxch.githubstore.feature.auth.presentation.AuthenticationViewModel
import zed.rainxch.githubstore.feature.home.data.repository.HomeRepositoryImpl
import zed.rainxch.githubstore.feature.home.data.repository.getPlatform
import zed.rainxch.githubstore.feature.home.domain.repository.HomeRepository
import zed.rainxch.githubstore.feature.home.presentation.HomeViewModel

// Core/shared modules
val coreModule: Module = module {
    single<TokenDataSource> { DefaultTokenDataSource() }
    // Authed GitHub client that reads token snapshot from TokenDataSource on every request
    single { buildAuthedGitHubHttpClient(get()) }

    viewModelOf(::MainViewModel)
}

val authModule: Module = module {
    single<AuthRepository> { AuthRepositoryImpl(tokenDataSource = get()) }

    // Use cases
    factory { StartDeviceFlowUseCase(get()) }
    factory { AwaitDeviceTokenUseCase(get()) }
    factory { ObserveAccessTokenUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // Presentation
    viewModel { AuthenticationViewModel(get(), get(), get(), get()) }
}

val homeModule: Module = module {
    single<HomeRepository> {
        HomeRepositoryImpl(
            githubNetworkClient = get(),
            platform = getPlatform()
        )
    }

    // Presentation
    viewModel { HomeViewModel(get(), get()) }
}
