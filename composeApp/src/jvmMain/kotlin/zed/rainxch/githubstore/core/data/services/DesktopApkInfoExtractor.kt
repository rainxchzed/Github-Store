package zed.rainxch.githubstore.core.data.services

import zed.rainxch.githubstore.core.domain.model.ApkPackageInfo

class DesktopApkInfoExtractor : ApkInfoExtractor {
    override suspend fun extractPackageInfo(filePath: String): ApkPackageInfo? {
        return null
    }
}