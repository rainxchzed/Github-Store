package zed.rainxch.githubstore.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import zed.rainxch.githubstore.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.githubstore.core.data.local.db.dao.UpdateHistoryDao
import zed.rainxch.githubstore.core.data.local.db.entities.InstallSource
import zed.rainxch.githubstore.core.data.local.db.entities.InstalledApp
import zed.rainxch.githubstore.core.data.local.db.entities.UpdateHistory
import zed.rainxch.githubstore.core.domain.repository.InstalledAppsRepository
import zed.rainxch.githubstore.core.data.services.Downloader
import zed.rainxch.githubstore.core.data.services.Installer
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository
import java.io.File

class InstalledAppsRepositoryImpl(
    private val dao: InstalledAppDao,
    private val historyDao: UpdateHistoryDao,
    private val detailsRepository: DetailsRepository,
    private val installer: Installer,
    private val downloader: Downloader
) : InstalledAppsRepository {

    override fun getAllInstalledApps(): Flow<List<InstalledApp>> = dao.getAllInstalledApps()

    override fun getAppsWithUpdates(): Flow<List<InstalledApp>> = dao.getAppsWithUpdates()

    override fun getUpdateCount(): Flow<Int> = dao.getUpdateCount()

    override suspend fun getAppByPackage(packageName: String): InstalledApp? =
        dao.getAppByPackage(packageName)

    override suspend fun getAppByRepoId(repoId: Long): InstalledApp? =
        dao.getAppByRepoId(repoId)

    override suspend fun isAppInstalled(repoId: Long): Boolean =
        dao.getAppByRepoId(repoId) != null

    override suspend fun saveInstalledApp(app: InstalledApp) {
        dao.insertApp(app)
    }

    override suspend fun deleteInstalledApp(packageName: String) {
        dao.deleteByPackageName(packageName)
    }

    override suspend fun checkForUpdates(packageName: String): Boolean {
        val app = dao.getAppByPackage(packageName) ?: return false

        try {
            val latestRelease = detailsRepository.getLatestPublishedRelease(
                owner = app.repoOwner,
                repo = app.repoName,
                defaultBranch = ""
            )

            if (latestRelease != null) {
                val normalizedInstalledTag = normalizeVersion(app.installedVersion)
                val normalizedLatestTag = normalizeVersion(latestRelease.tagName)

                if (normalizedInstalledTag == normalizedLatestTag) {
                    dao.updateVersionInfo(
                        packageName = packageName,
                        available = false,
                        version = latestRelease.tagName,
                        assetName = app.latestAssetName,
                        assetUrl = app.latestAssetUrl,
                        assetSize = app.latestAssetSize,
                        releaseNotes = latestRelease.description ?: "",
                        timestamp = System.currentTimeMillis(),
                        latestVersionName = app.latestVersionName,
                        latestVersionCode = app.latestVersionCode
                    )
                    return false
                }

                val installableAssets = latestRelease.assets.filter { asset ->
                    installer.isAssetInstallable(asset.name)
                }

                val primaryAsset = installer.choosePrimaryAsset(installableAssets)

                var isUpdateAvailable = true
                var latestVersionName: String? = null
                var latestVersionCode: Long? = null

                if (primaryAsset != null) {
                    val tempAssetName = primaryAsset.name + ".tmp"
                    downloader.download(primaryAsset.downloadUrl, tempAssetName).collect { }

                    val tempPath = downloader.getDownloadedFilePath(tempAssetName)
                    if (tempPath != null) {
                        val latestInfo =
                            installer.getApkInfoExtractor().extractPackageInfo(tempPath)
                        File(tempPath).delete()

                        if (latestInfo != null) {
                            latestVersionName = latestInfo.versionName
                            latestVersionCode = latestInfo.versionCode
                            isUpdateAvailable = latestVersionCode > app.installedVersionCode
                        } else {
                            isUpdateAvailable = false
                            latestVersionName = latestRelease.tagName
                        }
                    } else {
                        isUpdateAvailable = false
                        latestVersionName = latestRelease.tagName
                    }
                } else {
                    isUpdateAvailable = false
                    latestVersionName = latestRelease.tagName
                }

                Logger.d {
                    "Update check for ${app.appName}: currentTag=${app.installedVersion}, latestTag=${latestRelease.tagName}, " +
                            "currentCode=${app.installedVersionCode}, latestCode=$latestVersionCode, isUpdate=$isUpdateAvailable, " +
                            "primaryAsset=${primaryAsset?.name}"
                }

                dao.updateVersionInfo(
                    packageName = packageName,
                    available = isUpdateAvailable,
                    version = latestRelease.tagName,
                    assetName = primaryAsset?.name,
                    assetUrl = primaryAsset?.downloadUrl,
                    assetSize = primaryAsset?.size,
                    releaseNotes = latestRelease.description ?: "",
                    timestamp = System.currentTimeMillis(),
                    latestVersionName = latestVersionName,
                    latestVersionCode = latestVersionCode
                )

                return isUpdateAvailable
            }
        } catch (e: Exception) {
            Logger.e { "Failed to check updates for $packageName: ${e.message}" }
            dao.updateLastChecked(packageName, System.currentTimeMillis())
        }

        return false
    }

    override suspend fun checkAllForUpdates() {
        val apps = dao.getAllInstalledApps().first()
        apps.forEach { app ->
            if (app.updateCheckEnabled) {
                try {
                    checkForUpdates(app.packageName)
                } catch (e: Exception) {
                    Logger.w { "Failed to check updates for ${app.packageName}: ${e.message}" }
                }
            }
        }
    }

    override suspend fun updateAppVersion(
        packageName: String,
        newTag: String,
        newAssetName: String,
        newAssetUrl: String,
        newVersionName: String,
        newVersionCode: Long
    ) {
        val app = dao.getAppByPackage(packageName) ?: return

        Logger.d {
            "Updating app version: $packageName from ${app.installedVersion} to $newTag"
        }

        historyDao.insertHistory(
            UpdateHistory(
                packageName = packageName,
                appName = app.appName,
                repoOwner = app.repoOwner,
                repoName = app.repoName,
                fromVersion = app.installedVersion,
                toVersion = newTag,
                updatedAt = System.currentTimeMillis(),
                updateSource = InstallSource.THIS_APP,
                success = true
            )
        )

        dao.updateApp(
            app.copy(
                installedVersion = newTag,
                installedAssetName = newAssetName,
                installedAssetUrl = newAssetUrl,
                installedVersionName = newVersionName,
                installedVersionCode = newVersionCode,
                latestVersion = newTag,
                latestAssetName = newAssetName,
                latestAssetUrl = newAssetUrl,
                latestVersionName = newVersionName,
                latestVersionCode = newVersionCode,
                isUpdateAvailable = false,
                lastUpdatedAt = System.currentTimeMillis(),
                lastCheckedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateApp(app: InstalledApp) {
        dao.updateApp(app)
    }

    override suspend fun updatePendingStatus(packageName: String, isPending: Boolean) {
        val app = dao.getAppByPackage(packageName) ?: return
        dao.updateApp(app.copy(isPendingInstall = isPending))
    }

    private fun normalizeVersion(version: String): String {
        return version.removePrefix("v").removePrefix("V").trim()
    }
}