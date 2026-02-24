package zed.rainxch.settings.data.di

import org.koin.dsl.module
import zed.rainxch.settings.data.repository.SettingsRepositoryImpl
import zed.rainxch.settings.domain.repository.SettingsRepository

val settingsModule = module {
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            authenticationState = get(),
            tokenStore = get()
        )
    }
}