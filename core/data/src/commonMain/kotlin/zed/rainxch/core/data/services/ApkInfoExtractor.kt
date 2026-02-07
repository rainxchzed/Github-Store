package zed.rainxch.core.data.services

import zed.rainxch.core.domain.model.ApkPackageInfo

interface ApkInfoExtractor {
    suspend fun extractPackageInfo(filePath: String): ApkPackageInfo?
}