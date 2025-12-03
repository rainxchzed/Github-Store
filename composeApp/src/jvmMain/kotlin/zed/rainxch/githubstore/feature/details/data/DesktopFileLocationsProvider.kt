package zed.rainxch.githubstore.feature.details.data

import zed.rainxch.githubstore.core.domain.model.PlatformType
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

class DesktopFileLocationsProvider(
    private val platform: PlatformType
) : FileLocationsProvider {

    override fun appDownloadsDir(): String {
        val baseDir = when (platform) {
            PlatformType.WINDOWS -> {
                val appData = System.getenv("LOCALAPPDATA")
                    ?: (System.getProperty("user.home") + "\\AppData\\Local")
                File(appData, "GithubStore\\Downloads")
            }
            PlatformType.MACOS -> {
                val home = System.getProperty("user.home")
                File(home, "Library/Caches/GithubStore/Downloads")
            }
            PlatformType.LINUX -> {
                val cacheHome = System.getenv("XDG_CACHE_HOME")
                    ?: (System.getProperty("user.home") + "/.cache")
                File(cacheHome, "githubstore/downloads")
            }
            else -> {
                File(System.getProperty("user.home"), ".githubstore/downloads")
            }
        }
        
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        
        return baseDir.absolutePath
    }

    override fun setExecutableIfNeeded(path: String) {
        if (platform == PlatformType.LINUX || platform == PlatformType.MACOS) {
            try {
                val file = File(path)
                val filePath = file.toPath()

                val perms = Files.getPosixFilePermissions(filePath).toMutableSet()

                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                perms.add(PosixFilePermission.OTHERS_EXECUTE)

                Files.setPosixFilePermissions(filePath, perms)
            } catch (e: Exception) {
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", path)).waitFor()
                } catch (e2: Exception) {
                    println("Warning: Could not set executable permission on $path")
                }
            }
        }
    }
}