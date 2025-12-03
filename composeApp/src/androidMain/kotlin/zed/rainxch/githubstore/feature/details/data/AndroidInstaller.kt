package zed.rainxch.githubstore.feature.details.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.GithubAsset

class AndroidInstaller(
    private val context: Context,
) : Installer {

    override fun detectSystemArchitecture(): Architecture {
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: return Architecture.UNKNOWN
        return when {
            arch.contains("arm64") || arch.contains("aarch64") -> Architecture.AARCH64
            arch.contains("armeabi") -> Architecture.ARM
            arch.contains("x86_64") -> Architecture.X86_64
            arch.contains("x86") -> Architecture.X86
            else -> Architecture.UNKNOWN
        }
    }

    override fun isAssetInstallable(assetName: String): Boolean {
        val name = assetName.lowercase()

        // Only APK files are installable on Android
        if (!name.endsWith(".apk")) return false

        // Check architecture compatibility
        val systemArch = detectSystemArchitecture()
        return isArchitectureCompatible(name, systemArch)
    }

    private fun isArchitectureCompatible(assetName: String, systemArch: Architecture): Boolean {
        val name = assetName.lowercase()

        // If no architecture specified, assume universal APK
        val hasArchInName = listOf(
            "x86_64", "amd64", "x64",
            "aarch64", "arm64",
            "i386", "i686", "x86",
            "armv7", "armeabi", "arm"
        ).any { name.contains(it) }

        if (!hasArchInName) return true // Universal APK

        return when (systemArch) {
            Architecture.X86_64 -> {
                name.contains("x86_64") || name.contains("amd64") || name.contains("x64")
            }
            Architecture.AARCH64 -> {
                name.contains("aarch64") || name.contains("arm64")
            }
            Architecture.X86 -> {
                name.contains("i386") || name.contains("i686") || name.contains("x86")
            }
            Architecture.ARM -> {
                name.contains("armv7") || name.contains("armeabi") || name.contains("arm")
            }
            Architecture.UNKNOWN -> true // Allow all if we can't detect
        }
    }

    override fun choosePrimaryAsset(assets: List<GithubAsset>): GithubAsset? {
        if (assets.isEmpty()) return null

        val systemArch = detectSystemArchitecture()

        // Filter for compatible APKs
        val compatibleAssets = assets.filter { asset ->
            isArchitectureCompatible(asset.name.lowercase(), systemArch)
        }

        val assetsToConsider = compatibleAssets.ifEmpty { assets }

        // Prefer exact architecture match, then largest file
        return assetsToConsider.maxByOrNull { asset ->
            val name = asset.name.lowercase()
            val archBoost = when (systemArch) {
                Architecture.X86_64 -> {
                    if (name.contains("x86_64") || name.contains("amd64")) 10000 else 0
                }
                Architecture.AARCH64 -> {
                    if (name.contains("aarch64") || name.contains("arm64")) 10000 else 0
                }
                Architecture.X86 -> {
                    if (name.contains("i386") || name.contains("i686")) 10000 else 0
                }
                Architecture.ARM -> {
                    if (name.contains("armv7") || name.contains("armeabi")) 10000 else 0
                }
                Architecture.UNKNOWN -> 0
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
}