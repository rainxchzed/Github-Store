package zed.rainxch.details.data.di

import org.koin.dsl.module
import zed.rainxch.details.data.repository.DetailsRepositoryImpl
import zed.rainxch.details.domain.repository.DetailsRepository

val detailsModule = module {
    single<DetailsRepository> {
        DetailsRepositoryImpl(
            logger = get(),
            httpClient = get(),
            localizationManager = get()
        )
    }
}