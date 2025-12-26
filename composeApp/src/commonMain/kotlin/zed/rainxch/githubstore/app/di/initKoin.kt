package zed.rainxch.githubstore.app.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            platformModule,
            coreModule,
            authModule,
            homeModule,
            searchModule,
            detailsModule,
            settingsModule,
            appsModule,
            networkModule,
            tokenModule,
        )
    }
}