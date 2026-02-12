package zed.rainxch.core.data.services

import zed.rainxch.core.domain.model.SystemPackageInfo
import zed.rainxch.core.domain.system.PackageMonitor

class DesktopPackageMonitor : PackageMonitor {
    override suspend fun isPackageInstalled(packageName: String): Boolean {
        return false
    }

    override suspend fun getInstalledPackageInfo(packageName: String): SystemPackageInfo? {
        return null
    }

    override suspend fun getAllInstalledPackageNames(): Set<String> {
        return setOf()
    }

}