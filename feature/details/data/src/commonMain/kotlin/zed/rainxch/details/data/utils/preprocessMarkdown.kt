package zed.rainxch.details.data.utils

fun preprocessMarkdown(markdown: String, baseUrl: String): String {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    var processed = markdown
    var imageCount = 0
    var svgSkipped = 0

    fun normalizeGitHubUrl(url: String): String {
        return if (url.contains("github.com") && url.contains("/blob/")) {
            url.replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
        } else {
            url
        }
    }

    fun isSvgUrl(url: String): Boolean {
        return url.endsWith(".svg", ignoreCase = true) ||
                url.contains(".svg?", ignoreCase = true) ||
                url.contains(".svg#", ignoreCase = true)
    }

    processed = processed.replace(
        Regex(
            """<img\s+([^>]*?)\s*\/?>""",
            RegexOption.IGNORE_CASE
        )
    ) { imgMatch ->
        val imgTag = imgMatch.groupValues[1]

        val srcMatch = Regex("""src=(["'])([^"']+)\1""").find(imgTag)
        val src = srcMatch?.groupValues?.get(2) ?: ""

        val altMatch = Regex("""alt=(["'])([^"']*)\1""").find(imgTag)
        val alt = altMatch?.groupValues?.get(2) ?: ""

        if (src.isNotEmpty()) {
            val normalizedSrc = normalizeGitHubUrl(src)

            if (isSvgUrl(normalizedSrc)) {
                svgSkipped++

                if (alt.isNotEmpty()) {
                    "**$alt**"
                } else {
                    ""
                }
            } else {
                imageCount++
                "![$alt]($normalizedSrc)"
            }
        } else {
            ""
        }
    }

    processed = processed.replace(
        Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
    ) { match ->
        val alt = match.groupValues[1]
        val originalPath = match.groupValues[2]

        val isAbsolute = originalPath.startsWith("http://") ||
                originalPath.startsWith("https://") ||
                originalPath.startsWith("data:")

        val finalUrl = if (isAbsolute) {
            normalizeGitHubUrl(originalPath)
        } else {
            val path = originalPath.trim().trimStart('.', '/')
            "$normalizedBaseUrl$path"
        }

        if (isSvgUrl(finalUrl)) {
            svgSkipped++
            if (alt.isNotEmpty()) {
                "**$alt**"
            } else {
                ""
            }
        } else {
            imageCount++
            "![$alt]($finalUrl)"
        }
    }

    processed = processed.replace(
        Regex("""<div[^>]*?align=["']center["'][^>]*?>\s*""", RegexOption.IGNORE_CASE),
        "\n\n"
    )
    processed = processed.replace(
        Regex("""</div>\s*""", RegexOption.IGNORE_CASE),
        "\n\n"
    )

    processed = processed.replace(
        Regex("""<p[^>]*?>\s*</p>""", RegexOption.IGNORE_CASE),
        ""
    )

    processed = processed.replace(
        Regex("""\n{3,}"""),
        "\n\n"
    )

    processed = processed.replace(
        Regex("""^\]\([^)]+\)""", RegexOption.MULTILINE),
        ""
    )

    return processed
}