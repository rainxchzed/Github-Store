package zed.rainxch.githubstore.core.data.services

import kotlinx.coroutines.flow.Flow
import zed.rainxch.githubstore.core.domain.model.DownloadedFile
import zed.rainxch.githubstore.feature.details.domain.model.DownloadProgress

interface Downloader {

    fun download(url: String, suggestedFileName: String? = null): Flow<DownloadProgress>

    suspend fun saveToFile(url: String, suggestedFileName: String? = null): String

    suspend fun getDownloadedFilePath(fileName: String): String?

    suspend fun cancelDownload(fileName: String): Boolean
    suspend fun listDownloadedFiles(): List<DownloadedFile>
    suspend fun getLatestDownload(): DownloadedFile?
    suspend fun getLatestDownloadForAssets(assetNames: List<String>): DownloadedFile?

    // Add this new method
    suspend fun getFileSize(filePath: String): Long?
}
