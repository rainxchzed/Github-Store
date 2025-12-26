package zed.rainxch.githubstore.feature.apps.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.core.data.local.db.entities.InstalledApp
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.presentation.utils.AppLauncher
import zed.rainxch.githubstore.feature.apps.domain.repository.AppsRepository

class AppsRepositoryImpl(
    private val appLauncher: AppLauncher,
    private val installedAppsRepository: InstalledAppsRepository
) : AppsRepository {
    override suspend fun getApps(): Flow<List<InstalledApp>> {
        return installedAppsRepository.getAllInstalledApps()
    }

    override suspend fun openApp(
        installedApp: InstalledApp,
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