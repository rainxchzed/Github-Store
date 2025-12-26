package zed.rainxch.githubstore.core.data.services

import zed.rainxch.githubstore.core.domain.model.SystemPackageInfo

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