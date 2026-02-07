package zed.rainxch.core.presentation.utils

import zed.rainxch.core.domain.model.InstalledApp

interface AppLauncher {
    suspend fun launchApp(installedApp: InstalledApp): Result<Unit>
    suspend fun canLaunchApp(installedApp: InstalledApp): Boolean
}