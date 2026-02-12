package zed.rainxch.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import zed.rainxch.core.domain.model.RateLimitInfo
import zed.rainxch.core.domain.repository.RateLimitRepository
import kotlin.time.Clock

class RateLimitRepositoryImpl : RateLimitRepository {
    
    private val _rateLimitState = MutableStateFlow<RateLimitInfo?>(null)
    override val rateLimitState: StateFlow<RateLimitInfo?> = _rateLimitState.asStateFlow()

    override fun updateRateLimit(rateLimitInfo: RateLimitInfo?) {
        _rateLimitState.value = rateLimitInfo
    }

    override fun getCurrentRateLimit(): RateLimitInfo? {
        return _rateLimitState.value
    }

    override fun isCurrentlyLimited(): Boolean {
        val info = getCurrentRateLimit() ?: return false
        return info.isCurrentlyLimited()
    }

    override fun clear() {
        _rateLimitState.value = null
    }
}