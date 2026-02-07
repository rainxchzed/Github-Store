package zed.rainxch.details.presentation.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer

object MarkdownImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? {
        if (link.isBlank()) {
            Logger.d { "ImageTransformer: ⚠️ Empty link" }
            return null
        }

        val normalizedLink = if (link.contains("github.com") && link.contains("/blob/")) {
            link.replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
                .also {
                    Logger.d { "ImageTransformer: 🔄 GitHub blob->raw: $link -> $it" }
                }
        } else {
            link
        }

        if (normalizedLink.endsWith(".svg", ignoreCase = true) ||
            normalizedLink.contains(".svg?", ignoreCase = true) ||
            normalizedLink.contains(".svg#", ignoreCase = true)) {
            Logger.d { "ImageTransformer: ⚠️ SVG skipped: $normalizedLink" }
            return null
        }

        if (!normalizedLink.startsWith("http://") &&
            !normalizedLink.startsWith("https://") &&
            !normalizedLink.startsWith("data:")
        ) {
            Logger.w { "ImageTransformer: ⚠️ Invalid URL scheme: $normalizedLink" }
            return null
        }

        Logger.d { "ImageTransformer: 🔄 Loading: $normalizedLink" }

        val painter = rememberAsyncImagePainter(
            model = normalizedLink,
            onError = { state ->
                Logger.e { "ImageTransformer: ❌ Failed: $normalizedLink\nError: ${state.result.throwable?.message}" }
            },
            onSuccess = {
                Logger.d { "ImageTransformer: ✅ Success: $normalizedLink" }
            }
        )

        return ImageData(
            painter = painter,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Image",
            contentScale = ContentScale.Fit
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size {
        return painter.intrinsicSize
    }
}