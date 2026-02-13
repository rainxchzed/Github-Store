package zed.rainxch.githubstore.core.domain.model

object AssetArchitectureMatcher {
    private val universalRegex = Regex(
        pattern = """(^|[^a-z0-9])(universal|noarch|all-arch|fat)([^a-z0-9]|$)"""
    )
    private val x86_64Regex = Regex(
        pattern = """(^|[^a-z0-9])(x86_64|amd64|x64)([^a-z0-9]|$)"""
    )
    private val arm64Regex = Regex(
        pattern = """(^|[^a-z0-9])(aarch64|arm64|arm64-v8a|armv8a|armv8|arm-v8|v8a)([^a-z0-9]|$)"""
    )
    private val x86Regex = Regex(
        pattern = """(^|[^a-z0-9])(i386|i686|x86)([^a-z0-9]|$)"""
    )
    private val armRegex = Regex(
        pattern = """(^|[^a-z0-9])(armeabi-v7a|armeabi|armv7a|armv7|arm-v7|v7a|arm)([^a-z0-9]|$)"""
    )

    fun detectArchitecture(assetName: String): Architecture? {
        val name = assetName.lowercase().replace('_', '-')
        if (universalRegex.containsMatchIn(name)) return null
        if (x86_64Regex.containsMatchIn(name)) return Architecture.X86_64
        if (arm64Regex.containsMatchIn(name)) return Architecture.AARCH64
        if (x86Regex.containsMatchIn(name)) return Architecture.X86
        if (armRegex.containsMatchIn(name)) return Architecture.ARM
        return null
    }

    fun isCompatible(assetName: String, systemArch: Architecture): Boolean {
        val assetArch = detectArchitecture(assetName) ?: return true
        return when (systemArch) {
            Architecture.X86_64 -> assetArch == Architecture.X86_64 || assetArch == Architecture.X86
            Architecture.AARCH64 -> assetArch == Architecture.AARCH64 || assetArch == Architecture.ARM
            Architecture.X86 -> assetArch == Architecture.X86
            Architecture.ARM -> assetArch == Architecture.ARM
            Architecture.UNKNOWN -> true
        }
    }

    fun isExactMatch(assetName: String, systemArch: Architecture): Boolean {
        val assetArch = detectArchitecture(assetName) ?: return false
        return assetArch == systemArch
    }
}
