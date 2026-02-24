package zed.rainxch.core.data.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.Credentials
import zed.rainxch.core.domain.model.ProxyConfig

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            proxyConfig?.let { config ->
                val javaProxyType = when (config.type) {
                    ProxyConfig.ProxyType.HTTP -> Proxy.Type.HTTP
                    ProxyConfig.ProxyType.SOCKS -> Proxy.Type.SOCKS
                }
                proxy = Proxy(javaProxyType, InetSocketAddress(config.host, config.port))

                if (config.username != null) {
                    config {
                        proxyAuthenticator { _, response ->
                            response.request.newBuilder()
                                .header(
                                    "Proxy-Authorization",
                                    Credentials.basic(
                                        config.username!!,
                                        config.password.orEmpty()
                                    )
                                )
                                .build()
                        }
                    }
                }
            }
        }
    }
}