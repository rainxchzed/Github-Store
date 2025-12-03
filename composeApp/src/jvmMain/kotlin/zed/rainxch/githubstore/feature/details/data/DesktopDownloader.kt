package zed.rainxch.githubstore.feature.details.data

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.isSuccess
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import zed.rainxch.githubstore.feature.details.domain.model.DownloadProgress
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DesktopDownloader(
    private val http: HttpClient,
    private val files: FileLocationsProvider,
) : Downloader {

    override fun download(url: String, suggestedFileName: String?): Flow<DownloadProgress> = channelFlow {
        withContext(Dispatchers.IO) {
            val dir = File(files.appDownloadsDir())
            if (!dir.exists()) dir.mkdirs()

            val safeName = (suggestedFileName?.takeIf { it.isNotBlank() }
                ?: url.substringAfterLast('/')
                    .ifBlank { "asset-${UUID.randomUUID()}" })
            val outFile = File(dir, safeName)

            if (outFile.exists() && outFile.length() > 0) {
                Logger.d { "File already exists: ${outFile.absolutePath}" }
                trySend(DownloadProgress(outFile.length(), outFile.length(), 100))
                close()
                return@withContext
            }

            Logger.d { "Downloading: $url to ${outFile.absolutePath}" }

            val response: HttpResponse = http.get(url)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Download failed: HTTP ${response.status.value}")
            }

            val total = response.headers["Content-Length"]?.toLongOrNull()
            val channel = response.bodyAsChannel()

            FileOutputStream(outFile).use { fos ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L

                while (isActive) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    downloaded += read

                    val percent = if (total != null && total > 0) {
                        ((downloaded * 100L) / total).toInt()
                    } else null

                    trySend(DownloadProgress(downloaded, total, percent))
                }
                fos.flush()
            }

            Logger.d { "Download complete: ${outFile.absolutePath}" }

            trySend(DownloadProgress(total ?: outFile.length(), total, 100))
            close()
        }
    }

    override suspend fun saveToFile(url: String, suggestedFileName: String?): String = withContext(Dispatchers.IO) {
        val dir = File(files.appDownloadsDir())
        val safeName = (suggestedFileName?.takeIf { it.isNotBlank() }
            ?: url.substringAfterLast('/')
                .ifBlank { "asset-${UUID.randomUUID()}" })

        val outFile = File(dir, safeName)

        if (outFile.exists() && outFile.length() > 0) {
            Logger.d { "File already exists: ${outFile.absolutePath}" }
            return@withContext outFile.absolutePath
        }

        Logger.w { "saveToFile called but file doesn't exist, downloading..." }
        download(url, suggestedFileName).collect { }

        outFile.absolutePath
    }

    override suspend fun getDownloadedFilePath(fileName: String): String? = withContext(Dispatchers.IO) {
        val dir = File(files.appDownloadsDir())
        val file = File(dir, fileName)

        if (file.exists() && file.length() > 0) {
            file.absolutePath
        } else {
            null
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}