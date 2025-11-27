package zed.rainxch.githubstore.feature.details.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.feature.home.data.repository.PlatformType
import java.awt.Desktop
import java.io.File
import java.io.IOException

class DesktopInstaller(
    private val files: FileLocationsProvider,
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
                files.setExecutableIfNeeded(file.absolutePath)
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                } else {
                    val pb = ProcessBuilder(file.absolutePath)
                    pb.start()
                }
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