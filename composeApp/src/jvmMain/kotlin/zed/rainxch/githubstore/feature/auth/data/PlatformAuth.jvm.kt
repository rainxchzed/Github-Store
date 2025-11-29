package zed.rainxch.githubstore.feature.auth.data

import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.BuildConfig
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.prefs.Preferences

import java.io.File
import java.io.FileInputStream
import java.util.Properties

actual fun getGithubClientId(): String {
    val fromSys = System.getProperty("GITHUB_CLIENT_ID")?.trim().orEmpty()
    if (fromSys.isNotEmpty()) return fromSys

    val fromEnv = System.getenv("GITHUB_CLIENT_ID")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return BuildConfig.GITHUB_CLIENT_ID
}

private fun findLocalPropertiesValue(key: String): String? {
    return try {
        val maxDepth = 5
        var dir = File(System.getProperty("user.dir"))
        repeat(maxDepth) {
            val candidate = File(dir, "local.properties")
            if (candidate.exists()) {
                Properties().apply {
                    FileInputStream(candidate).use { load(it) }
                }.getProperty(key)?.let { return it }
            }
            dir = dir.parentFile ?: return null
        }
        null
    } catch (_: Throwable) { null }
}

actual fun copyToClipboard(label: String, text: String): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
        true
    } catch (_: Throwable) { false }
}

class DesktopTokenStore : TokenStore {
    private val prefs: Preferences = Preferences.userRoot().node("zed.rainxch.githubstore")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(token: DeviceTokenSuccess) {
        prefs.put("token", json.encodeToString(DeviceTokenSuccess.serializer(), token))
    }

    override suspend fun load(): DeviceTokenSuccess? {
        val raw = prefs.get("token", null) ?: return null
        return runCatching { json.decodeFromString(DeviceTokenSuccess.serializer(), raw) }.getOrNull()
    }

    override suspend fun clear() { prefs.remove("token") }
}
