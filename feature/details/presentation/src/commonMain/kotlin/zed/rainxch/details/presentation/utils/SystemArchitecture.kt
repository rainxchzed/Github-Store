package zed.rainxch.details.presentation.utils

import zed.rainxch.core.domain.model.AssetArchitectureMatcher
import zed.rainxch.core.domain.model.SystemArchitecture

fun extractArchitectureFromName(name: String): String? {
    return when (AssetArchitectureMatcher.detectArchitecture(name)) {
        SystemArchitecture.X86_64 -> "x86_64"
        SystemArchitecture.AARCH64 -> "aarch64"
        SystemArchitecture.X86 -> "i386"
        SystemArchitecture.ARM -> "arm"
        SystemArchitecture.UNKNOWN, null -> null
    }
}

fun isExactArchitectureMatch(assetName: String, systemArch: SystemArchitecture): Boolean {
    return AssetArchitectureMatcher.isExactMatch(assetName, systemArch)
}
