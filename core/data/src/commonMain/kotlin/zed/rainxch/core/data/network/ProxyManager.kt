package zed.rainxch.core.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import zed.rainxch.core.domain.model.ProxyConfig

object ProxyManager {
    private val _proxyConfig = MutableStateFlow<ProxyConfig?>(null)
    val currentProxyConfig = _proxyConfig.asStateFlow()

    fun setProxyConfig(
        config: ProxyConfig?
    ) {
        _proxyConfig.update { config }
    }
}