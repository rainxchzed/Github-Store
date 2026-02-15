package zed.rainxch.core.data.network.interceptor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.Headers
import io.ktor.util.AttributeKey
import zed.rainxch.core.domain.model.RateLimitException
import zed.rainxch.core.domain.model.RateLimitInfo
import zed.rainxch.core.domain.repository.RateLimitRepository

class RateLimitInterceptor(
    private val rateLimitRepository: RateLimitRepository
) {
    
    class Config {
        var rateLimitRepository: RateLimitRepository? = null
    }

    companion object Plugin : HttpClientPlugin<Config, RateLimitInterceptor> {
        override val key: AttributeKey<RateLimitInterceptor> =
            AttributeKey("RateLimitInterceptor")

        override fun prepare(block: Config.() -> Unit): RateLimitInterceptor {
            val config = Config().apply(block)
            return RateLimitInterceptor(
                rateLimitRepository = requireNotNull(config.rateLimitRepository) {
                    "RateLimitRepository must be provided"
                }
            )
        }

        override fun install(plugin: RateLimitInterceptor, scope: HttpClient) {
            scope.receivePipeline.intercept(HttpReceivePipeline.State) {
                val response = subject

                parseRateLimitFromHeaders(response.headers)?.let { rateLimitInfo ->
                    plugin.rateLimitRepository.updateRateLimit(rateLimitInfo)

                    if (response.status.value == 403 && rateLimitInfo.isExhausted) {
                        throw RateLimitException(rateLimitInfo)
                    }
                }
                
                proceedWith(subject)
            }
        }
        
        private fun parseRateLimitFromHeaders(headers: Headers): RateLimitInfo? {
            return try {
                val limit = headers["X-RateLimit-Limit"]?.toIntOrNull() ?: return null
                val remaining = headers["X-RateLimit-Remaining"]?.toIntOrNull() ?: return null
                val reset = headers["X-RateLimit-Reset"]?.toLongOrNull() ?: return null
                val resource = headers["X-RateLimit-Resource"] ?: "core"

                RateLimitInfo(
                    limit = limit,
                    remaining = remaining,
                    resetTimestamp = reset,
                    resource = resource
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
