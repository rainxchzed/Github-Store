package zed.rainxch.core.data.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import zed.rainxch.core.domain.utils.ClipboardHelper

class AndroidClipboardHelper(
    private val context: Context
) : ClipboardHelper {
    override fun copy(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}