package zed.rainxch.core.domain.use_cases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.system.PackageMonitor

/**
 * Use case for synchronizing installed apps state with the system package manager.
 * 
 * Responsibilities:
 * 1. Remove apps from DB that are no longer installed on the system
 * 2. Migrate legacy apps missing versionName/versionCode fields
 * 
 * This should be called before loading or refreshing app data to ensure consistency.
 */
class SyncInstalledAppsUseCase(
    private val packageMonitor: PackageMonitor,
    private val installedAppsRepository: InstalledAppsRepository,
    private val platform: Platform,
    private val logger: GitHubStoreLogger
) {
    /**
     * Executes the sync operation.
     * 
     * @return Result indicating success or failure with error details
     */
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val installedPackageNames = packageMonitor.getAllInstalledPackageNames()
            val appsInDb = installedAppsRepository.getAllInstalledApps().first()

            val toDelete = mutableListOf<String>()
            val toMigrate = mutableListOf<Pair<String, MigrationResult>>()

            appsInDb.forEach { app ->
                when {
                    !installedPackageNames.contains(app.packageName) -> {
                        toDelete.add(app.packageName)
                    }

                    app.installedVersionName == null -> {
                        val migrationResult = determineMigrationData(app)
                        toMigrate.add(app.packageName to migrationResult)
                    }
                }
            }

            executeInTransaction {
                toDelete.forEach { packageName ->
                    try {
                        installedAppsRepository.deleteInstalledApp(packageName)
                        logger.info("Removed uninstalled app: $packageName")
                    } catch (e: Exception) {
                        logger.error("Failed to delete $packageName: ${e.message}")
                    }
                }

                toMigrate.forEach { (packageName, migrationResult) ->
                    try {
                        val app = appsInDb.find { it.packageName == packageName } ?: return@forEach

                        installedAppsRepository.updateApp(
                            app.copy(
                                installedVersionName = migrationResult.versionName,
                                installedVersionCode = migrationResult.versionCode,
                                latestVersionName = migrationResult.versionName,
                                latestVersionCode = migrationResult.versionCode
                            )
                        )

                        logger.info(
                            "Migrated $packageName: ${migrationResult.source} " +
                                    "(versionName=${migrationResult.versionName}, code=${migrationResult.versionCode})"
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to migrate $packageName: ${e.message}")
                    }
                }
            }

            logger.info(
                "Sync completed: ${toDelete.size} deleted, ${toMigrate.size} migrated"
            )

            Result.success(Unit)

        } catch (e: Exception) {
            logger.error("Sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun determineMigrationData(app: InstalledApp): MigrationResult {
        return if (platform == Platform.ANDROID) {
            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
            if (systemInfo != null) {
                MigrationResult(
                    versionName = systemInfo.versionName,
                    versionCode = systemInfo.versionCode,
                    source = "system package manager"
                )
            } else {
                MigrationResult(
                    versionName = app.installedVersion,
                    versionCode = 0L,
                    source = "fallback to release tag"
                )
            }
        } else {
            MigrationResult(
                versionName = app.installedVersion,
                versionCode = 0L,
                source = "desktop fallback to release tag"
            )
        }
    }

    private data class MigrationResult(
        val versionName: String,
        val versionCode: Long,
        val source: String
    )
}

suspend fun executeInTransaction(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        throw e
    }
}