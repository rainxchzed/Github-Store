package zed.rainxch.core.domain.model

class RateLimitException(
    val rateLimitInfo: RateLimitInfo,
) : Exception()
