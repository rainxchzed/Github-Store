package zed.rainxch.githubstore.core.presentation.utils

interface AppLauncher {
    suspend fun launchApp(installedApp: zed.rainxch.core.data.local.db.entities.InstalledAppEntity): Result<Unit>
    suspend fun canLaunchApp(installedApp: zed.rainxch.core.data.local.db.entities.InstalledAppEntity): Boolean
}