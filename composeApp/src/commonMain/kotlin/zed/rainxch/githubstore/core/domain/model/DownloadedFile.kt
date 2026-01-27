package zed.rainxch.githubstore.core.domain.model

data class DownloadedFile(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long
)