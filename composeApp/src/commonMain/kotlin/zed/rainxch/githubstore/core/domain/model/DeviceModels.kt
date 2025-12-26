@file:OptIn(ExperimentalTime::class)

package zed.rainxch.githubstore.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class DeviceStart(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    @SerialName("interval") val intervalSec: Int = 5,
    @SerialName("expires_in") val expiresInSec: Int
)

@Serializable
data class DeviceTokenSuccess(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    val createdAt: Long = Clock.System.now().epochSeconds,
    val platform: ApiPlatform? = null
) {
    fun isExpiredOrExpiringSoon(): Boolean {
        if (expiresIn == null) return false
        val expiresAt = createdAt + expiresIn
        val now = Clock.System.now().epochSeconds
        return now >= (expiresAt - 300)
    }

    fun isExpired(): Boolean {
        if (expiresIn == null) return false
        val expiresAt = createdAt + expiresIn
        return Clock.System.now().epochSeconds >= expiresAt
    }
}


@Serializable
data class DeviceTokenError(
    @SerialName("error") val error: String,
    @SerialName("error_description") val errorDescription: String? = null
)
