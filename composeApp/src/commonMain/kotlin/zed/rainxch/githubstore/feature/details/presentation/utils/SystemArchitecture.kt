package zed.rainxch.githubstore.feature.details.presentation.utils

import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.AssetArchitectureMatcher

fun extractArchitectureFromName(name: String): String? {
    return when (AssetArchitectureMatcher.detectArchitecture(name)) {
        Architecture.X86_64 -> "x86_64"
        Architecture.AARCH64 -> "aarch64"
        Architecture.X86 -> "i386"
        Architecture.ARM -> "arm"
        Architecture.UNKNOWN, null -> null
    }
}

fun isExactArchitectureMatch(assetName: String, systemArch: Architecture): Boolean {
    return AssetArchitectureMatcher.isExactMatch(assetName, systemArch)
}
