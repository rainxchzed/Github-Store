package zed.rainxch.githubstore.core.presentation.utils

import android.content.Intent
import android.content.Context
import androidx.core.net.toUri

object AppContextHolder {
    lateinit var appContext: Context
}

actual fun openBrowser(
    url: String,
    onError: (error: String) -> Unit
) {
    val ctx = AppContextHolder.appContext
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}
