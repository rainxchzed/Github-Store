package zed.rainxch.core.data.services

import zed.rainxch.core.domain.utils.ClipboardHelper
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class DesktopClipboardHelper : ClipboardHelper {
    override fun copy(label: String, text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}