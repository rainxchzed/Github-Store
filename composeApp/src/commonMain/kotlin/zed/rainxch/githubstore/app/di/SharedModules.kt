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