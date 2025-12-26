package zed.rainxch.githubstore.core.data.services

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import zed.rainxch.githubstore.feature.details.domain.model.DownloadProgress
import java.util.concurrent.ConcurrentHashMap
import androidx.core.net.toUri

class AndroidDownloader(
    private val context: Context,
    private val files: FileLocationsProvider
) : Downloader {

    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private val activeDownloads = ConcurrentHashMap<String, Long>()

    override fun download(url: String, suggestedFileName: String?): Flow<DownloadProgress> = flow {
        val dirPath = files.appDownloadsDir()
        val dir = File(dirPath)
        if (!dir.exists()) dir.mkdirs()

        val safeName = (suggestedFileName?.takeIf { it.isNotBlank() }
            ?: url.substringAfterLast('/').ifBlank { "asset-${UUID.randomUUID()}.apk" })

        val tentativeDestination = File(dir, safeName)

        if (tentativeDestination.exists()) {
            Logger.d { "Deleting existing file before download: ${tentativeDestination.absolutePath}" }
            tentativeDestination.delete()
        }

        Logger.d { "Starting download: $url" }

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(safeName)
            setDescription("Downloading asset")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

            setDestinationInExternalFilesDir(context, null, "ghs_downloads/$safeName")

            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        val downloadId = downloadManager.enqueue(request)
        activeDownloads[safeName] = downloadId

        try {
            var isDone = false
            while (!isDone && currentCoroutineContext().isActive) {
                val cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val percent = if (total > 0) ((downloaded * 100L) / total).toInt() else null

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val localUriStr =
                                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            val finalPath = Uri.parse(localUriStr).path
                                ?: throw IllegalStateException("Invalid local URI: $localUriStr")

                            val file = File(finalPath)

                            var attempts = 0
                            while ((!file.exists() || file.length() == 0L) && attempts < 6) {
                                delay(500L)
                                attempts++
                                emit(DownloadProgress(downloaded, total, 100))
                            }
                            if (!file.exists() || file.length() == 0L) {
                                throw IllegalStateException("File not ready after timeout: $finalPath")
                            }

                            Logger.d { "Download complete: $finalPath" }
                            emit(DownloadProgress(downloaded, total, 100))
                            isDone = true
                        }

                        DownloadManager.STATUS_FAILED -> {
                            val reason =
                                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            Logger.e { "Download failed with reason: $reason" }
                            throw IllegalStateException("Download failed: $reason")
                        }

                        else -> emit(
                            DownloadProgress(
                                downloaded,
                                if (total >= 0) total else null,
                                percent
                            )
                        )
                    }
                } else {
                    throw IllegalStateException("Download ID not found: $downloadId")
                }
                cursor.close()

                if (!isDone) delay(500L)
            }
        } finally {
            activeDownloads.remove(safeName)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveToFile(url: String, suggestedFileName: String?): String =
        withContext(Dispatchers.IO) {
            val safeName = (suggestedFileName?.takeIf { it.isNotBlank() }
                ?: url.substringAfterLast('/').ifBlank { "asset-${UUID.randomUUID()}.apk" })

            val file = File(files.appDownloadsDir(), safeName)

            if (file.exists()) {
                Logger.d { "Deleting existing file before download: ${file.absolutePath}" }
                file.delete()
            }

            Logger.d { "saveToFile downloading file..." }
            download(url, suggestedFileName).collect { }

            file.absolutePath
        }

    override suspend fun getDownloadedFilePath(fileName: String): String? =
        withContext(Dispatchers.IO) {
            val file = File(files.appDownloadsDir(), fileName)

            if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        }

    override suspend fun cancelDownload(fileName: String): Boolean =
        withContext(Dispatchers.IO) {

            var cancelled = false
            var deleted = false

            activeDownloads[fileName]?.let { downloadId: Long ->
                val removedCount = downloadManager.remove(downloadId)
                cancelled = removedCount > 0
                activeDownloads.remove(fileName)
            }

            val file = File(files.appDownloadsDir(), fileName)
            if (file.exists()) {
                deleted = file.delete()
            }

            cancelled || deleted
        }
}