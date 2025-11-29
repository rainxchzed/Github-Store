package zed.rainxch.githubstore.feature.details.data

import zed.rainxch.githubstore.core.domain.model.Architecture
import zed.rainxch.githubstore.core.domain.model.GithubAsset

/** High-level installer API used by the Details feature. Implemented per platform and bound via DI. */
interface Installer {
    /** Whether this platform can handle installing/opening this asset by extension or MIME. */
    suspend fun isSupported(extOrMime: String): Boolean

    /** Ensure any required permissions/settings are granted (may throw). */
    suspend fun ensurePermissionsOrThrow(extOrMime: String)

    /** Launch platform installation/open of the given file path. */
    suspend fun install(filePath: String, extOrMime: String)

    fun isAssetInstallable(assetName: String): Boolean
    fun choosePrimaryAsset(assets: List<GithubAsset>): GithubAsset?
    fun detectSystemArchitecture(): Architecture
}