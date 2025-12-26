package zed.rainxch.githubstore.feature.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import zed.rainxch.githubstore.BuildConfig
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceTokenSuccess

class AndroidTokenStore(
    private val dataStore: DataStore<Preferences>,
) : TokenStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun tokenKey(apiPlatform: ApiPlatform) =
        stringPreferencesKey("token_${apiPlatform.name.lowercase()}")

    override suspend fun save(apiPlatform: ApiPlatform, token: DeviceTokenSuccess) {
        val jsonString = json.encodeToString(DeviceTokenSuccess.serializer(), token)
        dataStore.edit { preferences ->
            preferences[tokenKey(apiPlatform)] = jsonString
        }
    }

    override suspend fun load(apiPlatform: ApiPlatform): DeviceTokenSuccess? {
        return runCatching {
            val preferences = dataStore.data.first()
            val raw = preferences[tokenKey(apiPlatform)]
                ?: return null  // ✅ FIXED: use tokenKey(apiPlatform)
            json.decodeFromString(DeviceTokenSuccess.serializer(), raw)
        }.getOrNull()
    }

    override suspend fun clear(apiPlatform: ApiPlatform) {
        dataStore.edit { it.remove(tokenKey(apiPlatform)) }  // ✅ FIXED: use tokenKey(apiPlatform)
    }
}

actual fun getGithubClientId(): String = BuildConfig.GITHUB_CLIENT_ID
actual fun getGitLabClientId(): String = BuildConfig.GITLAB_CLIENT_ID

actual fun getGitLabClientSecret(): String = BuildConfig.GITLAB_CLIENT_SECRET