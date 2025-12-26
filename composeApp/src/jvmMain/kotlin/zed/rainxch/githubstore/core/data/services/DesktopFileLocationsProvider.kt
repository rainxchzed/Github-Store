package zed.rainxch.githubstore.core.data.services

import co.touchlab.kermit.Logger
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

    override fun userDownloadsDir(): String {
        val downloadsDir = when (platform) {
            PlatformType.WINDOWS -> {
                val userProfile = System.getenv("USERPROFILE")
                    ?: System.getProperty("user.home")
                File(userProfile, "Downloads")
            }
            PlatformType.MACOS -> {
                val home = System.getProperty("user.home")
                File(home, "Downloads")
            }
            PlatformType.LINUX -> {
                val xdgDownloads = getXdgDownloadsDir()
                if (xdgDownloads != null) {
                    File(xdgDownloads)
                } else {
                    val home = System.getProperty("user.home")
                    File(home, "Downloads")
                }
            }
            else -> {
                File(System.getProperty("user.home"), "Downloads")
            }
        }

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        return downloadsDir.absolutePath
    }

    private fun getXdgDownloadsDir(): String? {
        return try {
            val userDirsFile = File(
                System.getProperty("user.home"),
                ".config/user-dirs.dirs"
            )

            if (userDirsFile.exists()) {
                userDirsFile.readLines().forEach { line ->
                    if (line.trim().startsWith("XDG_DOWNLOAD_DIR=")) {
                        val path = line.substringAfter("=")
                            .trim()
                            .removeSurrounding("\"")
                            .replace("\$HOME", System.getProperty("user.home"))
                        return path
                    }
                }
            }
            null
        } catch (e: Exception) {
            Logger.w { "Failed to read XDG user dirs: ${e.message}" }
            null
        }
    }
}