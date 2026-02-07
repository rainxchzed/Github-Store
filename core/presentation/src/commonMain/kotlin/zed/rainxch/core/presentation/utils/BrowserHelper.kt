package zed.rainxch.core.presentation.utils

interface BrowserHelper {
    fun openUrl(
        url: String,
        onFailure: (error: String) -> Unit = { },
    )
}

