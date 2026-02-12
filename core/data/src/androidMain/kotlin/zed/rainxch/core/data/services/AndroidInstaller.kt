package zed.rainxch.core.data.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import zed.rainxch.core.domain.model.SystemArchitecture
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.core.domain.system.InstallerInfoExtractor

class AndroidInstaller(
    private val context: Context,
    private val installerInfoExtractor: InstallerInfoExtractor
) : Installer {

    override fun getApkInfoExtractor(): InstallerInfoExtractor {
        return installerInfoExtractor
    }
    override fun detectSystemArchitecture(): SystemArchitecture {
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: return SystemArchitecture.UNKNOWN
        return when {
            arch.contains("arm64") || arch.contains("aarch64") -> SystemArchitecture.AARCH64
            arch.contains("armeabi") -> SystemArchitecture.ARM
            arch.contains("x86_64") -> SystemArchitecture.X86_64
            arch.contains("x86") -> SystemArchitecture.X86
            else -> SystemArchitecture.UNKNOWN
        }
    }

    override fun isAssetInstallable(assetName: String): Boolean {
        val name = assetName.lowercase()
        if (!name.endsWith(".apk")) return false
        val systemArch = detectSystemArchitecture()
        return isArchitectureCompatible(name, systemArch)
    }

    private fun isArchitectureCompatible(assetName: String, systemArch: SystemArchitecture): Boolean {
        val name = assetName.lowercase()
        val hasArchInName = listOf(
            "x86_64", "amd64", "x64",
            "aarch64", "arm64",
            "i386", "i686", "x86",
            "armv7", "armeabi", "arm"
        ).any { name.contains(it) }

        if (!hasArchInName) return true

        return when (systemArch) {
            SystemArchitecture.X86_64 -> {
                name.contains("x86_64") || name.contains("amd64") || name.contains("x64")
            }
            SystemArchitecture.AARCH64 -> {
                name.contains("aarch64") || name.contains("arm64")
            }
            SystemArchitecture.X86 -> {
                name.contains("i386") || name.contains("i686") || name.contains("x86")
            }
            SystemArchitecture.ARM -> {
                name.contains("armv7") || name.contains("armeabi") || name.contains("arm")
            }
            SystemArchitecture.UNKNOWN -> true
        }
    }

    override fun choosePrimaryAsset(assets: List<GithubAsset>): GithubAsset? {
        if (assets.isEmpty()) return null
        val systemArch = detectSystemArchitecture()
        val compatibleAssets = assets.filter { asset ->
            isArchitectureCompatible(asset.name.lowercase(), systemArch)
        }
        val assetsToConsider = compatibleAssets.ifEmpty { assets }
        return assetsToConsider.maxByOrNull { asset ->
            val name = asset.name.lowercase()
            val archBoost = when (systemArch) {
                SystemArchitecture.X86_64 -> {
                    if (name.contains("x86_64") || name.contains("amd64")) 10000 else 0
                }
                SystemArchitecture.AARCH64 -> {
                    if (name.contains("aarch64") || name.contains("arm64")) 10000 else 0
                }
                SystemArchitecture.X86 -> {
                    if (name.contains("i386") || name.contains("i686")) 10000 else 0
                }
                SystemArchitecture.ARM -> {
                    if (name.contains("armv7") || name.contains("armeabi")) 10000 else 0
                }
                SystemArchitecture.UNKNOWN -> 0
            }
            archBoost + asset.size
        }
    }

    override suspend fun isSupported(extOrMime: String): Boolean {
        val ext = extOrMime.lowercase().removePrefix(".")
        return ext == "apk"
    }

    override suspend fun ensurePermissionsOrThrow(extOrMime: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                throw IllegalStateException("Please enable 'Install unknown apps' for this app in Settings and try again.")
            }
        }
    }

    override suspend fun install(filePath: String, extOrMime: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalStateException("APK file not found: $filePath")
        }

        Logger.d { "Installing APK: $filePath" }

        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Logger.d { "APK installation intent launched" }
        } else {
            throw IllegalStateException("No installer available on this device")
        }
    }

    override fun isObtainiumInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("dev.imranr.obtainium.fdroid", 0)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo("dev.imranr.obtainium", 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun openInObtainium(
        repoOwner: String,
        repoName: String,
        onOpenInstaller: () -> Unit
    ) {
        val obtainiumUrl = "obtainium://add/https://github.com/$repoOwner/$repoName"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = obtainiumUrl.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            onOpenInstaller()
        }
    }

    override fun isAppManagerInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("io.github.muntashirakon.AppManager", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun openInAppManager(
        filePath: String,
        onOpenInstaller: () -> Unit
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalStateException("APK file not found: $filePath")
        }

        Logger.d { "Opening APK in AppManager: $filePath" }

        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            setPackage("io.github.muntashirakon.AppManager")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            Logger.d { "APK opened in AppManager" }
        } catch (_: ActivityNotFoundException) {
            onOpenInstaller()
        }
    }
}