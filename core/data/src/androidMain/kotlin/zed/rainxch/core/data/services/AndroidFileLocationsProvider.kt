package zed.rainxch.core.data.services

import android.content.Context
import java.io.File

class AndroidFileLocationsProvider(
    private val context: Context
) : zed.rainxch.core.data.services.FileLocationsProvider {
    override fun appDownloadsDir(): String {
        val externalFilesRoot = context.getExternalFilesDir(null)
        val dir = File(externalFilesRoot, "ghs_downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun userDownloadsDir(): String {
        return "" // No-op
    }

    override fun setExecutableIfNeeded(path: String) {
        // No-op on Android
    }
}