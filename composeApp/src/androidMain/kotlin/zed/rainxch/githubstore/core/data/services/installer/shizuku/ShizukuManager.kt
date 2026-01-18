package zed.rainxch.githubstore.core.data.services.installer.shizuku

import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val context: Context
) {

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _binderStatus = MutableStateFlow(BinderStatus.UNKNOWN)
    val binderStatus: StateFlow<BinderStatus> = _binderStatus.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Logger.d { "Shizuku binder received" }
        _binderStatus.value = BinderStatus.RECEIVED
        checkAvailability()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Logger.w { "Shizuku binder dead" }
        _binderStatus.value = BinderStatus.DEAD
        _isAvailable.value = false
        _hasPermission.value = false
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Logger.d { "Shizuku permission result: code=$requestCode, granted=${grantResult == PackageManager.PERMISSION_GRANTED}" }
            if (requestCode == PERMISSION_REQUEST_CODE) {
                _hasPermission.value = grantResult == PackageManager.PERMISSION_GRANTED
            }
        }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        checkAvailability()
    }

    fun checkAvailability() {
        try {
            val isRunning = Shizuku.pingBinder()
            _isAvailable.value = isRunning

            if (isRunning) {
                _binderStatus.value = BinderStatus.ALIVE
                val hasPermission = checkPermission()
                _hasPermission.value = hasPermission
                Logger.d { "Shizuku available: running=$isRunning, hasPermission=$hasPermission" }
            } else {
                _binderStatus.value = BinderStatus.DEAD
                _hasPermission.value = false
                Logger.d { "Shizuku not running" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Error checking Shizuku availability" }
            _isAvailable.value = false
            _hasPermission.value = false
            _binderStatus.value = BinderStatus.ERROR
        }
    }

    private fun checkPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                // Pre-v11 doesn't need permission
                true
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Logger.e(e) { "Error checking Shizuku permission" }
            false
        }
    }

    fun requestPermission(): Boolean {
        return try {
            if (checkPermission()) {
                _hasPermission.value = true
                true
            } else {
                Logger.d { "Requesting Shizuku permission" }
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                false
            }
        } catch (e: Exception) {
            Logger.e(e) { "Error requesting Shizuku permission" }
            false
        }
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getShizukuVersion(): Int {
        return try {
            Shizuku.getVersion()
        } catch (e: Exception) {
            -1
        }
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Logger.e(e) { "Error cleaning up Shizuku listeners" }
        }
    }

    enum class BinderStatus {
        UNKNOWN,
        RECEIVED,
        ALIVE,
        DEAD,
        ERROR
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}