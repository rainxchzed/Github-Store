package zed.rainxch.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetTimestamp: Long,
    val resource: String = "core"
) {
    val isExhausted: Boolean
        get() = remaining == 0

    fun timeUntilReset(currentTimeSeconds: Long): Duration {
        val diff = resetTimestamp - currentTimeSeconds
        return diff.seconds.coerceAtLeast(Duration.ZERO)
    }

    fun isCurrentlyLimited(currentTimeSeconds: Long): Boolean {
        return isExhausted && timeUntilReset(currentTimeSeconds) > Duration.ZERO
    }
}