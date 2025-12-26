@file:OptIn(ExperimentalTime::class)

package zed.rainxch.githubstore.network

import io.ktor.http.*
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val reset: Instant,
    val resource: String = "core",
    val apiPlatform: ApiPlatform
) {
    val isExhausted: Boolean get() = remaining == 0

    fun timeUntilReset(): Long {
        val now = Clock.System.now()
        return (reset - now).inWholeMilliseconds.coerceAtLeast(0)
    }

    companion object {
        fun fromHeaders(headers: Headers, apiPlatform: ApiPlatform): RateLimitInfo? {
            return try {
                when (apiPlatform) {
                    ApiPlatform.Github -> fromGitHubHeaders(headers, apiPlatform)
                    ApiPlatform.GitLab -> fromGitLabHeaders(headers, apiPlatform)
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun fromGitHubHeaders(headers: Headers, apiPlatform: ApiPlatform): RateLimitInfo? {
            val limit = headers["X-RateLimit-Limit"]?.toIntOrNull() ?: return null
            val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull() ?: return null
            val reset = headers["X-RateLimit-Reset"]?.toLongOrNull()?.let {
                Instant.fromEpochSeconds(it)
            } ?: return null
            val resource = headers["X-RateLimit-Resource"] ?: "core"

            return RateLimitInfo(limit, remaining, reset, resource, apiPlatform)
        }

        private fun fromGitLabHeaders(headers: Headers, apiPlatform: ApiPlatform): RateLimitInfo? {
            val limit = headers["RateLimit-Limit"]?.toIntOrNull() ?: return null
            val remaining = headers["RateLimit-Remaining"]?.toIntOrNull() ?: return null
            val reset = headers["RateLimit-Reset"]?.toLongOrNull()?.let {
                Instant.fromEpochSeconds(it)
            } ?: return null
            val resource = "api"

            return RateLimitInfo(limit, remaining, reset, resource, apiPlatform)
        }
    }
}

class RateLimitException(
    val rateLimitInfo: RateLimitInfo,
    message: String = "${rateLimitInfo.apiPlatform.name} API rate limit exceeded"
) : Exception(message)

class RateLimitHandler {
    private val rateLimits = mutableMapOf<ApiPlatform, RateLimitInfo>()

    fun isRateLimited(apiPlatform: ApiPlatform): Boolean {
        val info = rateLimits[apiPlatform] ?: return false
        if (!info.isExhausted) return false

        return info.timeUntilReset() > 0
    }

    fun getTimeUntilReset(apiPlatform: ApiPlatform): Long {
        return rateLimits[apiPlatform]?.timeUntilReset() ?: 0L
    }

    fun updateFromHeaders(headers: Headers, apiPlatform: ApiPlatform) {
        RateLimitInfo.fromHeaders(headers, apiPlatform)?.let {
            rateLimits[apiPlatform] = it
        }
    }

    fun getCurrentRateLimit(apiPlatform: ApiPlatform): RateLimitInfo? = rateLimits[apiPlatform]

    fun clearRateLimit(apiPlatform: ApiPlatform) {
        rateLimits.remove(apiPlatform)
    }
}