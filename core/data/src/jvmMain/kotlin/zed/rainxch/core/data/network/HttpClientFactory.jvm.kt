package zed.rainxch.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import zed.rainxch.core.domain.model.ProxyConfig

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient {
    return HttpClient(CIO) {
        engine {
            proxy = proxyConfig?.let { config ->
                when (config.type) {
                    ProxyConfig.ProxyType.HTTP -> ProxyBuilder.http(
                        Url("http://${config.host}:${config.port}")
                    )
                    ProxyConfig.ProxyType.SOCKS -> ProxyBuilder.socks(
                        config.host, config.port
                    )
                }
            }
        }
    }
}