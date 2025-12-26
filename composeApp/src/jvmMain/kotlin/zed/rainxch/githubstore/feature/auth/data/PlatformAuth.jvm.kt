package zed.rainxch.githubstore.feature.auth.data

import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.BuildConfig
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.prefs.Preferences

class DesktopTokenStore : TokenStore {
    private val prefs: Preferences = Preferences.userRoot().node("zed.rainxch.githubstore")
    private val json = Json { ignoreUnknownKeys = true }

    private fun prefKey(apiPlatform: ApiPlatform) = "token_${apiPlatform.name.lowercase()}"

    override suspend fun save(apiPlatform: ApiPlatform, token: DeviceTokenSuccess) {
        prefs.put(prefKey(apiPlatform), json.encodeToString(DeviceTokenSuccess.serializer(), token))
    }

    override suspend fun load(apiPlatform: ApiPlatform): DeviceTokenSuccess? {
        val raw = prefs.get(prefKey(apiPlatform), null)
            ?: return null  // ✅ FIXED: use prefKey(apiPlatform)
        return runCatching {
            json.decodeFromString(DeviceTokenSuccess.serializer(), raw)
        }.getOrNull()
    }

    override suspend fun clear(apiPlatform: ApiPlatform) {
        prefs.remove(prefKey(apiPlatform))  // ✅ FIXED: use prefKey(apiPlatform)
    }
}


actual fun getGithubClientId(): String {
    val fromSys = System.getProperty("GITHUB_CLIENT_ID")?.trim().orEmpty()
    if (fromSys.isNotEmpty()) return fromSys

    val fromEnv = System.getenv("GITHUB_CLIENT_ID")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return BuildConfig.GITHUB_CLIENT_ID
}

actual fun getGitLabClientId(): String {
    val fromSys = System.getProperty("GITLAB_CLIENT_ID")?.trim().orEmpty()
    if (fromSys.isNotEmpty()) return fromSys

    val fromEnv = System.getenv("GITLAB_CLIENT_ID")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return BuildConfig.GITLAB_CLIENT_ID
}

actual fun getGitLabClientSecret(): String {
    val fromSys = System.getProperty("GITLAB_CLIENT_SECRET")?.trim().orEmpty()
    if (fromSys.isNotEmpty()) return fromSys

    val fromEnv = System.getenv("GITLAB_CLIENT_SECRET")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return BuildConfig.GITLAB_CLIENT_SECRET
}