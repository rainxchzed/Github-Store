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
                // Use AppData/Local for Windows
                val appData = System.getenv("LOCALAPPDATA")
                    ?: (System.getProperty("user.home") + "\\AppData\\Local")
                File(appData, "GithubStore\\Downloads")
            }
            PlatformType.MACOS -> {
                // Use ~/Library/Caches for macOS
                val home = System.getProperty("user.home")
                File(home, "Library/Caches/GithubStore/Downloads")
            }
            PlatformType.LINUX -> {
                // Use XDG_CACHE_HOME or ~/.cache for Linux
                val cacheHome = System.getenv("XDG_CACHE_HOME")
                    ?: (System.getProperty("user.home") + "/.cache")
                File(cacheHome, "githubstore/downloads")
            }
            else -> {
                // Fallback to user.home
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
                
                // Get current permissions
                val perms = Files.getPosixFilePermissions(filePath).toMutableSet()
                
                // Add executable permissions
                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                perms.add(PosixFilePermission.OTHERS_EXECUTE)
                
                // Set new permissions
                Files.setPosixFilePermissions(filePath, perms)
            } catch (e: Exception) {
                // Fallback to Runtime.exec for older Java versions or Windows
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", path)).waitFor()
                } catch (e2: Exception) {
                    // Log but don't fail - some platforms might not support this
                    println("Warning: Could not set executable permission on $path")
                }
            }
        }
        // On Windows, .exe/.msi files are automatically executable
    }
}