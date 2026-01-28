package zed.rainxch.githubstore.core.data.services

import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.core.data.model.InstallationProgress
import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.GithubAsset
import java.io.File

interface Installer {
    suspend fun isSupported(extOrMime: String): Boolean

    suspend fun ensurePermissionsOrThrow(extOrMime: String)

    suspend fun install(filePath: String, extOrMime: String)

    fun isAssetInstallable(assetName: String): Boolean
    fun choosePrimaryAsset(assets: List<GithubAsset>): GithubAsset?
    fun detectSystemArchitecture(): Architecture

    fun isObtainiumInstalled(): Boolean
    fun openInObtainium(
        repoOwner: String,
        repoName: String,
        onOpenInstaller: () -> Unit
    )
    fun openShizukuApp()

    fun isAppManagerInstalled(): Boolean
    fun openInAppManager(
        filePath: String,
        onOpenInstaller: () -> Unit
    )

    fun getApkInfoExtractor(): ApkInfoExtractor

    fun isShizukuAvailable(): Boolean = false

    fun isShizukuInstalled(): Boolean = false

    fun requestShizukuPermission(): Boolean = false

    fun installWithShizukuProgress(file: File): Flow<InstallationProgress> {
        throw UnsupportedOperationException("Shizuku installation not supported on this platform")
    }

    suspend fun uninstallWithShizuku(packageName: String): Boolean = false
}