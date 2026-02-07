package zed.rainxch.apps.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.feature.apps.domain.repository.AppsRepository

class AppsRepositoryImpl(
    private val appLauncher: AppLauncher,
    private val appsRepository: InstalledAppsRepository
) : AppsRepository {
    override suspend fun getApps(): Flow<List<zed.rainxch.core.data.local.db.entities.InstalledAppEntity>> {
        return appsRepository.getAllInstalledApps()
    }

    override suspend fun openApp(
        installedApp: zed.rainxch.core.data.local.db.entities.InstalledAppEntity,
        onCantLaunchApp: () -> Unit
    ) {
        val canLaunch = appLauncher.canLaunchApp(installedApp)

        if (canLaunch) {
            appLauncher.launchApp(installedApp)
                .onFailure { error ->
                    Logger.e { "Failed to launch app: ${error.message}" }
                    onCantLaunchApp()
                }
        } else {
            onCantLaunchApp()
        }
    }
}