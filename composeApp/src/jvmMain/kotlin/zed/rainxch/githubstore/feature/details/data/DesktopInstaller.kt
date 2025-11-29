package zed.rainxch.githubstore.feature.details.data

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.GithubAsset
import zed.rainxch.githubstore.core.domain.model.PlatformType
import zed.rainxch.githubstore.feature.details.data.model.LinuxTerminal
import zed.rainxch.githubstore.feature.details.domain.model.LinuxPackageType
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException

class DesktopInstaller(
    private val platform: PlatformType
) : Installer {

    private val linuxPackageType: LinuxPackageType by lazy {
        determineLinuxPackageType()
    }

    private val systemArchitecture: Architecture by lazy {
        determineSystemArchitecture()
    }

    override fun detectSystemArchitecture(): Architecture = systemArchitecture

    override fun isAssetInstallable(assetName: String): Boolean {
        val name = assetName.lowercase()

        val hasValidExtension = when (platform) {
            PlatformType.ANDROID -> name.endsWith(".apk")
            PlatformType.WINDOWS -> name.endsWith(".msi") || name.endsWith(".exe")
            PlatformType.MACOS -> name.endsWith(".dmg") || name.endsWith(".pkg")
            PlatformType.LINUX -> {
                when {
                    name.endsWith(".appimage") -> true
                    name.endsWith(".deb") -> linuxPackageType == LinuxPackageType.DEB
                    name.endsWith(".rpm") -> linuxPackageType == LinuxPackageType.RPM
                    else -> false
                }
            }
        }

        if (!hasValidExtension) return false

        return isArchitectureCompatible(name, systemArchitecture)
    }

    override fun choosePrimaryAsset(assets: List<GithubAsset>): GithubAsset? {
        if (assets.isEmpty()) return null

        val priority = when (platform) {
            PlatformType.ANDROID -> listOf(".apk")
            PlatformType.WINDOWS -> listOf(".msi", ".exe")
            PlatformType.MACOS -> listOf(".dmg", ".pkg")
            PlatformType.LINUX -> {
                when (linuxPackageType) {
                    LinuxPackageType.DEB -> listOf(".deb", ".appimage")
                    LinuxPackageType.RPM -> listOf(".rpm", ".appimage")
                    LinuxPackageType.UNIVERSAL -> listOf(".appimage")
                }
            }
        }

        val compatibleAssets = assets.filter { asset ->
            isArchitectureCompatible(asset.name.lowercase(), systemArchitecture)
        }

        val assetsToConsider = compatibleAssets.ifEmpty { assets }

        return assetsToConsider.maxByOrNull { asset ->
            val name = asset.name.lowercase()
            val idx = priority.indexOfFirst { name.endsWith(it) }
                .let { if (it == -1) 999 else it }

            val archBoost = if (isExactArchitectureMatch(name, systemArchitecture)) 10000 else 0

            archBoost + (-1000 * (priority.size - idx)) + asset.size
        }
    }

    private fun determineSystemArchitecture(): Architecture {
        if (platform == PlatformType.MACOS) {
            try {
                val process = ProcessBuilder("uname", "-m").start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()

                return when (output) {
                    "arm64" -> Architecture.AARCH64
                    "x86_64" -> Architecture.X86_64
                    else -> Architecture.fromString(System.getProperty("os.arch"))
                }
            } catch (_: Exception) { }
        }

        val osArch = System.getProperty("os.arch") ?: return Architecture.UNKNOWN
        return Architecture.fromString(osArch)
    }

    private fun determineLinuxPackageType(): LinuxPackageType {
        if (platform != PlatformType.LINUX) return LinuxPackageType.UNIVERSAL

        return try {
            if (commandExists("apt")) {
                return LinuxPackageType.DEB
            }

            if (commandExists("dnf")) {
                return LinuxPackageType.RPM
            }

            if (commandExists("yum")) {
                return LinuxPackageType.RPM
            }

            if (commandExists("zypper")) {
                return LinuxPackageType.RPM
            }

            LinuxPackageType.UNIVERSAL
        } catch (e: Exception) {
            Logger.w { "Failed to detect Linux package type: ${e.message}" }
            LinuxPackageType.UNIVERSAL
        }
    }

    private fun commandExists(command: String): Boolean {
        return try {
            val process = ProcessBuilder("which", command).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun isArchitectureCompatible(assetName: String, systemArch: Architecture): Boolean {
        val name = assetName.lowercase()

        if (platform == PlatformType.MACOS) {
            if (name.contains("universal") || name.contains("darwin")) {
                return true
            }

            if ((name.endsWith(".dmg") || name.endsWith(".pkg")) &&
                !listOf("x86_64", "amd64", "arm64", "aarch64").any { name.contains(it) }) {
                return true
            }
        }

        val hasArchInName = listOf(
            "x86_64", "amd64", "x64",
            "aarch64", "arm64",
            "i386", "i686", "x86",
            "armv7", "arm"
        ).any { name.contains(it) }

        if (!hasArchInName) return true

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
                name.contains("armv7") || name.contains("arm")
            }

            Architecture.UNKNOWN -> true
        }
    }

    private fun isExactArchitectureMatch(assetName: String, systemArch: Architecture): Boolean {
        val name = assetName.lowercase()
        return when (systemArch) {
            Architecture.X86_64 -> name.contains("x86_64") || name.contains("amd64") || name.contains(
                "x64"
            )

            Architecture.AARCH64 -> name.contains("aarch64") || name.contains("arm64")
            Architecture.X86 -> name.contains("i386") || name.contains("i686")
            Architecture.ARM -> name.contains("armv7") || name.contains("arm")
            Architecture.UNKNOWN -> false
        }
    }

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

                tryShowNotification(
                    title = "Installation Started",
                    message = "Please drag the application to your Applications folder"
                )
            }

            "pkg" -> {
                try {
                    val script = """
                    do shell script "installer -pkg '${file.absolutePath}' -target /" with administrator privileges
                """.trimIndent()

                    val pb = ProcessBuilder("osascript", "-e", script)
                    val process = pb.start()
                    val exitCode = process.waitFor()

                    if (exitCode != 0) {
                        throw IOException("Installation cancelled or failed")
                    }

                    Logger.d { "PKG installed successfully" }
                } catch (e: Exception) {
                    Logger.w { "Automated install failed, opening installer GUI: ${e.message}" }
                    ProcessBuilder("open", file.absolutePath).start()
                }
            }

            else -> throw IllegalArgumentException("Unsupported macOS installer: .$ext")
        }
    }

    private fun tryShowNotification(title: String, message: String) {
        if (platform == PlatformType.MACOS) {
            try {
                val script = """
                display notification "$message" with title "$title"
            """.trimIndent()
                ProcessBuilder("osascript", "-e", script).start()
            } catch (e: Exception) {
                Logger.w { "Could not show macOS notification: ${e.message}" }
            }
        } else {
            try {
                ProcessBuilder(
                    "notify-send",
                    title,
                    message,
                    "-u", "critical",
                    "-t", "10000"
                ).start()
            } catch (e: Exception) {
                Logger.w { "Could not show notification: ${e.message}" }
            }
        }
    }

    private fun installLinux(file: File, ext: String) {
        when (ext) {
            "appimage" -> {
                installAppImage(file)
            }

            "deb" -> {
                installDebPackage(file)
            }

            "rpm" -> {
                installRpmPackage(file)
            }

            else -> throw IllegalArgumentException("Unsupported Linux installer: .$ext")
        }
    }

    private fun installDebPackage(file: File) {
        Logger.d { "Installing DEB package: ${file.absolutePath}" }

        val installMethods = listOf(
            listOf("pkexec", "apt", "install", "-y", file.absolutePath),

            listOf("pkexec", "sh", "-c", "dpkg -i '${file.absolutePath}' || apt-get install -f -y"),

            listOf("gdebi-gtk", file.absolutePath),

            null
        )

        for (method in installMethods) {
            if (method == null) {
                openTerminalForDebInstall(file.absolutePath)
                return
            }

            try {
                Logger.d { "Trying installation method: ${method.joinToString(" ")}" }
                val process = ProcessBuilder(method).start()
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    Logger.d { "DEB package installed successfully" }
                    return
                } else {
                    Logger.w { "Installation method failed with exit code: $exitCode" }
                }
            } catch (e: IOException) {
                Logger.w { "Installation method not available: ${e.message}" }
            }
        }

        throw IOException("Could not install DEB package. Please install it manually.")
    }

    private fun installRpmPackage(file: File) {
        Logger.d { "Installing RPM package: ${file.absolutePath}" }

        val installMethods = listOf(
            listOf("pkexec", "dnf", "install", "-y", file.absolutePath),

            listOf("pkexec", "yum", "install", "-y", file.absolutePath),

            listOf("pkexec", "zypper", "install", "-y", file.absolutePath),

            listOf("pkexec", "rpm", "-i", file.absolutePath),

            null
        )

        for (method in installMethods) {
            if (method == null) {
                openTerminalForRpmInstall(file.absolutePath)
                return
            }

            try {
                Logger.d { "Trying installation method: ${method.joinToString(" ")}" }
                val process = ProcessBuilder(method).start()
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    Logger.d { "RPM package installed successfully" }
                    return
                } else {
                    Logger.w { "Installation method failed with exit code: $exitCode" }
                }
            } catch (e: IOException) {
                Logger.w { "Installation method not available: ${e.message}" }
            }
        }

        throw IOException("Could not install RPM package. Please install it manually.")
    }

    private fun openTerminalForDebInstall(filePath: String) {
        Logger.d { "Opening terminal for DEB installation" }

        val availableTerminals = detectAvailableTerminals()

        if (availableTerminals.isEmpty()) {
            Logger.e { "No terminal emulator found on system" }

            tryShowNotification(
                "Installation Required",
                "Please install manually: sudo dpkg -i '$filePath' && sudo apt-get install -f -y"
            )

            tryCopyToClipboard("sudo dpkg -i '$filePath' && sudo apt-get install -f -y")

            throw IOException(
                "No terminal emulator found. Please install manually:\n" +
                        "sudo dpkg -i '$filePath' && sudo apt-get install -f -y"
            )
        }

        val command =
            "echo 'Installing DEB package...'; sudo dpkg -i '$filePath' && sudo apt-get install -f -y; echo ''; echo 'Installation complete. Press Enter to close...'; read"

        for (terminal in availableTerminals) {
            try {
                Logger.d { "Trying terminal: ${terminal.name}" }
                val processBuilder = when (terminal) {
                    LinuxTerminal.GNOME_TERMINAL -> ProcessBuilder(
                        "gnome-terminal", "--", "bash", "-c", command
                    )

                    LinuxTerminal.KONSOLE -> ProcessBuilder(
                        "konsole", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.XTERM -> ProcessBuilder(
                        "xterm", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.XFCE4_TERMINAL -> ProcessBuilder(
                        "xfce4-terminal", "-e", "bash -c \"$command\""
                    )

                    LinuxTerminal.ALACRITTY -> ProcessBuilder(
                        "alacritty", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.KITTY -> ProcessBuilder(
                        "kitty", "bash", "-c", command
                    )

                    LinuxTerminal.TILIX -> ProcessBuilder(
                        "tilix", "-e", "bash -c \"$command\""
                    )

                    LinuxTerminal.MATE_TERMINAL -> ProcessBuilder(
                        "mate-terminal", "-e", "bash -c \"$command\""
                    )
                }

                processBuilder.start()
                Logger.d { "Terminal opened successfully: ${terminal.name}" }
                return
            } catch (e: IOException) {
                Logger.w { "Failed to open ${terminal.name}: ${e.message}" }
            }
        }

        throw IOException("Could not open any terminal emulator")
    }

    private fun openTerminalForRpmInstall(filePath: String) {
        Logger.d { "Opening terminal for RPM installation" }

        val availableTerminals = detectAvailableTerminals()

        if (availableTerminals.isEmpty()) {
            Logger.e { "No terminal emulator found on system" }

            tryShowNotification(
                "Installation Required",
                "Please install manually: sudo dnf install -y '$filePath'"
            )

            tryCopyToClipboard("sudo dnf install -y '$filePath'")

            throw IOException(
                "No terminal emulator found. Please install manually:\n" +
                        "sudo dnf install -y '$filePath'"
            )
        }

        val command ="echo 'Installing RPM package...'; " +
                "sudo dnf install -y '$filePath' " +
                "|| sudo yum install -y '$filePath' " +
                "|| sudo rpm -i '$filePath'; echo ''; " +
                "echo 'Installation complete. Press Enter to close...'; read"

        for (terminal in availableTerminals) {
            try {
                Logger.d { "Trying terminal: ${terminal.name}" }
                val processBuilder = when (terminal) {
                    LinuxTerminal.GNOME_TERMINAL -> ProcessBuilder(
                        "gnome-terminal", "--", "bash", "-c", command
                    )

                    LinuxTerminal.KONSOLE -> ProcessBuilder(
                        "konsole", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.XTERM -> ProcessBuilder(
                        "xterm", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.XFCE4_TERMINAL -> ProcessBuilder(
                        "xfce4-terminal", "-e", "bash -c \"$command\""
                    )

                    LinuxTerminal.ALACRITTY -> ProcessBuilder(
                        "alacritty", "-e", "bash", "-c", command
                    )

                    LinuxTerminal.KITTY -> ProcessBuilder(
                        "kitty", "bash", "-c", command
                    )

                    LinuxTerminal.TILIX -> ProcessBuilder(
                        "tilix", "-e", "bash -c \"$command\""
                    )

                    LinuxTerminal.MATE_TERMINAL -> ProcessBuilder(
                        "mate-terminal", "-e", "bash -c \"$command\""
                    )
                }

                processBuilder.start()
                Logger.d { "Terminal opened successfully: ${terminal.name}" }
                return
            } catch (e: IOException) {
                Logger.w { "Failed to open ${terminal.name}: ${e.message}" }
            }
        }

        throw IOException("Could not open any terminal emulator")
    }


    private fun detectAvailableTerminals(): List<LinuxTerminal> {
        val linuxTerminals = mutableListOf<LinuxTerminal>()

        val linuxTerminalCommands = mapOf(
            LinuxTerminal.GNOME_TERMINAL to "gnome-terminal",
            LinuxTerminal.KONSOLE to "konsole",
            LinuxTerminal.XTERM to "xterm",
            LinuxTerminal.XFCE4_TERMINAL to "xfce4-terminal",
            LinuxTerminal.ALACRITTY to "alacritty",
            LinuxTerminal.KITTY to "kitty",
            LinuxTerminal.TILIX to "tilix",
            LinuxTerminal.MATE_TERMINAL to "mate-terminal"
        )

        for ((terminal, command) in linuxTerminalCommands) {
            try {
                val process = ProcessBuilder("which", command).start()
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    linuxTerminals.add(terminal)
                    Logger.d { "Found terminal: $command" }
                }
            } catch (_: Exception) {
            }
        }

        return linuxTerminals
    }

    private fun tryCopyToClipboard(text: String) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            Logger.d { "Command copied to clipboard" }
        } catch (e: Exception) {
            Logger.w { "Could not copy to clipboard: ${e.message}" }
        }
    }


    private fun installAppImage(file: File) {
        Logger.d { "Installing AppImage: ${file.absolutePath}" }

        val desktopDir = getDesktopDirectory()
        Logger.d { "Desktop directory: ${desktopDir.absolutePath}" }
        Logger.d { "Desktop exists: ${desktopDir.exists()}, isDirectory: ${desktopDir.isDirectory}, canWrite: ${desktopDir.canWrite()}" }

        val destinationFile = File(desktopDir, file.name)

        val finalDestination = if (destinationFile.exists()) {
            Logger.d { "File already exists, generating unique name" }
            generateUniqueFileName(desktopDir, file.name)
        } else {
            destinationFile
        }

        Logger.d { "Final destination: ${finalDestination.absolutePath}" }

        try {
            Logger.d { "Copying file..." }
            file.copyTo(finalDestination, overwrite = false)
            Logger.d { "Copy successful, file size: ${finalDestination.length()} bytes" }

            val executableSet = finalDestination.setExecutable(true, false)
            Logger.d { "Set executable: $executableSet" }

            if (!finalDestination.exists()) {
                throw IllegalStateException("File was copied but doesn't exist at destination")
            }

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
        } catch (_: Exception) {
        }

        val homeDir = System.getProperty("user.home")
        val desktopCandidates = listOf(
            File(homeDir, "Desktop"),
            File(homeDir, "desktop"),
            File(homeDir, ".local/share/Desktop"),
            File(homeDir)
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

}