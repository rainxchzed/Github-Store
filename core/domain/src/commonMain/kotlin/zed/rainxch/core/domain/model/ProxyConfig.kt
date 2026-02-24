package zed.rainxch.core.domain.model

data class ProxyConfig(
    val type: ProxyType,
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
) {
    enum class ProxyType { HTTP, SOCKS }
}