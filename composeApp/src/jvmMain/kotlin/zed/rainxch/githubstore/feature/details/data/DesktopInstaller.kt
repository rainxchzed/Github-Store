package zed.rainxch.githubstore.feature.details.data

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.core.domain.model.PlatformType
import java.awt.Desktop
import java.io.File
import java.io.IOException

class DesktopInstaller(
    private val platform: PlatformType
) : Installer {

    override suspend fun isSupported(extOrMime: String): Boolean {
        val ext = extOrMime.lowercase().removePrefix(".")
        return when (platform) {
            PlatformType.WINDOWS -> ext in listOf("msi", "exe")
            PlatformType.MACOS -> ext in listOf("dmg", "pkg")
            PlatformType.LINUX -> ext in listOf("appimage", "deb", "rpm")
            else -> false
        }
    }

    override suspend fun ensurePermissionsOrThrow(extOrMime: String) = withContext(Dispatchers.IO) {
        val ext = extOrMime.lowercase().removePrefix(".")

        if (platform == PlatformType.LINUX && ext == "appimage") {
            try {
                val tempFile = File.createTempFile("appimage_perm_test", ".tmp")
                try {
                    val canSetExecutable = tempFile.setExecutable(true)
                    if (!canSetExecutable) {
                        throw IllegalStateException(
                            "Unable to set executable permissions. AppImage installation requires " +
                                    "the ability to make files executable."
                        )
                    }
                } finally {
                    tempFile.delete()
                }
            } catch (e: IOException) {
                throw IllegalStateException(
                    "Failed to verify permission capabilities for AppImage installation: ${e.message}",
                    e
                )
            } catch (e: SecurityException) {
                throw IllegalStateException(
                    "Security restrictions prevent setting executable permissions for AppImage files.",
                    e
                )
            }
        }
    }

    override suspend fun install(filePath: String, extOrMime: String) =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalStateException("File not found: $filePath")
            }

            val ext = extOrMime.lowercase().removePrefix(".")

            when (platform) {
                PlatformType.WINDOWS -> installWindows(file, ext)
                PlatformType.MACOS -> installMacOS(file, ext)
                PlatformType.LINUX -> installLinux(file, ext)
                else -> throw UnsupportedOperationException("Installation not supported on $platform")
            }
        }

    private fun installWindows(file: File, ext: String) {
        when (ext) {
            "msi" -> {
                val pb = ProcessBuilder("msiexec", "/i", file.absolutePath)
                pb.start()
            }

            "exe" -> {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                } else {
                    val pb = ProcessBuilder(file.absolutePath)
                    pb.start()
                }
            }

            else -> throw IllegalArgumentException("Unsupported Windows installer: .$ext")
        }
    }

    private fun installMacOS(file: File, ext: String) {
        when (ext) {
            "dmg" -> {
                val pb = ProcessBuilder("open", file.absolutePath)
                pb.start()
            }

            "pkg" -> {
                val pb = ProcessBuilder("open", file.absolutePath)
                pb.start()
            }

            else -> throw IllegalArgumentException("Unsupported macOS installer: .$ext")
        }
    }

    private fun installLinux(file: File, ext: String) {
        when (ext) {
            "appimage" -> {
                installAppImage(file)
            }

            "deb" -> {
                try {
                    val pb = ProcessBuilder("gdebi-gtk", file.absolutePath)
                    pb.start()
                } catch (e: IOException) {
                    try {
                        val pb = ProcessBuilder("xdg-open", file.absolutePath)
                        pb.start()
                    } catch (e2: IOException) {
                        openTerminalForPackageInstall("dpkg", file.absolutePath)
                    }
                }
            }

            "rpm" -> {
                try {
                    val pb = ProcessBuilder("xdg-open", file.absolutePath)
                    pb.start()
                } catch (e: IOException) {
                    openTerminalForPackageInstall("rpm", file.absolutePath)
                }
            }

            else -> throw IllegalArgumentException("Unsupported Linux installer: .$ext")
        }
    }

    private fun installAppImage(file: File) {
        Logger.d { "Installing AppImage: ${file.absolutePath}" }

        // Get Desktop directory
        val desktopDir = getDesktopDirectory()
        Logger.d { "Desktop directory: ${desktopDir.absolutePath}" }
        Logger.d { "Desktop exists: ${desktopDir.exists()}, isDirectory: ${desktopDir.isDirectory}, canWrite: ${desktopDir.canWrite()}" }

        // Copy file to desktop with its original name
        val destinationFile = File(desktopDir, file.name)

        // If file already exists on desktop, add a number suffix
        val finalDestination = if (destinationFile.exists()) {
            Logger.d { "File already exists, generating unique name" }
            generateUniqueFileName(desktopDir, file.name)
        } else {
            destinationFile
        }

        Logger.d { "Final destination: ${finalDestination.absolutePath}" }

        try {
            // Copy the file
            Logger.d { "Copying file..." }
            file.copyTo(finalDestination, overwrite = false)
            Logger.d { "Copy successful, file size: ${finalDestination.length()} bytes" }

            // Make it executable
            val executableSet = finalDestination.setExecutable(true, false)
            Logger.d { "Set executable: $executableSet" }

            // Verify the file exists
            if (!finalDestination.exists()) {
                throw IllegalStateException("File was copied but doesn't exist at destination")
            }

            // Optionally, try to open the desktop folder to show the file
            try {
                Logger.d { "Attempting to open desktop folder..." }
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(desktopDir)
                    Logger.d { "Desktop folder opened" }
                } else {
                    Logger.w { "Desktop not supported, trying xdg-open" }
                    ProcessBuilder("xdg-open", desktopDir.absolutePath).start()
                }
            } catch (e: Exception) {
                Logger.w { "Could not open desktop folder: ${e.message}" }
                // Not a critical error, just log it
            }

            Logger.d { "AppImage installation completed successfully" }
        } catch (e: IOException) {
            Logger.e { "Failed to copy AppImage: ${e.message}" }
            e.printStackTrace()
            throw IllegalStateException(
                "Failed to copy AppImage to desktop: ${e.message}. " +
                        "Desktop path: ${desktopDir.absolutePath}. " +
                        "Please ensure you have write permissions to your Desktop folder.",
                e
            )
        } catch (e: SecurityException) {
            Logger.e { "Security exception: ${e.message}" }
            e.printStackTrace()
            throw IllegalStateException(
                "Security restrictions prevent copying AppImage to desktop.",
                e
            )
        } catch (e: Exception) {
            Logger.e { "Unexpected error: ${e.message}" }
            e.printStackTrace()
            throw IllegalStateException("Failed to install AppImage: ${e.message}", e)
        }
    }

    private fun getDesktopDirectory(): File {
        // Try XDG user dirs first (most reliable on modern Linux)
        try {
            val process = ProcessBuilder("xdg-user-dir", "DESKTOP").start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (output.isNotEmpty() && output != "DESKTOP") {
                val xdgDesktop = File(output)
                if (xdgDesktop.exists() && xdgDesktop.isDirectory) {
                    return xdgDesktop
                }
            }
        } catch (e: Exception) {
            // Fall through to alternatives
        }

        // Fallback to common Desktop locations
        val homeDir = System.getProperty("user.home")
        val desktopCandidates = listOf(
            File(homeDir, "Desktop"),
            File(homeDir, "desktop"),
            File(homeDir, ".local/share/Desktop"),
            File(homeDir) // Last resort: home directory
        )

        return desktopCandidates.firstOrNull { it.exists() && it.isDirectory }
            ?: File(homeDir, "Desktop").also { it.mkdirs() }
    }

    private fun generateUniqueFileName(directory: File, originalName: String): File {
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        val extension = originalName.substringAfterLast(".", "")

        var counter = 1
        var candidateFile: File

        do {
            val newName = if (extension.isNotEmpty()) {
                "${nameWithoutExtension}_$counter.$extension"
            } else {
                "${nameWithoutExtension}_$counter"
            }
            candidateFile = File(directory, newName)
            counter++
        } while (candidateFile.exists() && counter < 1000)

        if (candidateFile.exists()) {
            throw IllegalStateException("Could not generate unique filename on desktop")
        }

        return candidateFile
    }

    private fun openTerminalForPackageInstall(packageManager: String, filePath: String) {
        val terminals = listOf(
            listOf(
                "gnome-terminal",
                "--",
                "bash",
                "-c",
                "sudo $packageManager -i $filePath; read -p 'Press Enter to close...'"
            ),
            listOf(
                "konsole",
                "-e",
                "bash",
                "-c",
                "sudo $packageManager -i $filePath; read -p 'Press Enter to close...'"
            ),
            listOf(
                "xterm",
                "-e",
                "bash",
                "-c",
                "sudo $packageManager -i $filePath; read -p 'Press Enter to close...'"
            )
        )

        for (terminalCmd in terminals) {
            try {
                val pb = ProcessBuilder(terminalCmd)
                pb.start()
                return
            } catch (e: IOException) {
            }
        }

        throw IOException("Could not find a terminal emulator to run package installation")
    }
}