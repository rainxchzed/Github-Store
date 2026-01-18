package zed.rainxch.githubstore.core.data.services.installer.shizuku

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume

class ShizukuInstaller(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {

    fun installApk(apkFile: File): Flow<InstallProgress> = flow {
        if (!shizukuManager.isAvailable.value || !shizukuManager.hasPermission.value) {
            emit(InstallProgress.Error("Shizuku not available or no permission"))
            return@flow
        }

        if (!apkFile.exists()) {
            emit(InstallProgress.Error("APK file not found: ${apkFile.absolutePath}"))
            return@flow
        }

        try {
            emit(InstallProgress.Preparing)
            Logger.d { "Installing via Shizuku: ${apkFile.name}" }

            val result = installApkInternal(apkFile) { progress ->
                when (progress) {
                    is SessionProgress.Creating -> emit(InstallProgress.CreatingSession(0))
                    is SessionProgress.Writing -> emit(InstallProgress.WritingApk(progress.percent))
                    is SessionProgress.Committing -> emit(InstallProgress.Committing)
                }
            }

            if (result.success) {
                emit(InstallProgress.Success(result.packageName ?: ""))
                Logger.d { "Installation successful: ${result.packageName}" }
            } else {
                emit(InstallProgress.Error(result.errorMessage ?: "Installation failed"))
                Logger.e { "Installation failed: ${result.errorMessage}" }
            }

        } catch (e: Exception) {
            Logger.e(e) { "Failed to install via Shizuku" }
            emit(InstallProgress.Error(e.message ?: "Installation failed"))
        }
    }

    private suspend fun installApkInternal(
        apkFile: File,
        onProgress: suspend (SessionProgress) -> Unit
    ): InstallResult = withContext(Dispatchers.IO) {
        val packageInstaller = context.packageManager.packageInstaller
        var sessionId = -1

        try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
                }

                try {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        0
                    )
                    packageInfo?.applicationInfo?.let { appInfo ->
                        appInfo.sourceDir = apkFile.absolutePath
                        appInfo.publicSourceDir = apkFile.absolutePath
                        val label = context.packageManager.getApplicationLabel(appInfo).toString()
                        setAppLabel(label)
                    }
                } catch (e: Exception) {
                    Logger.w(e) { "Failed to set app label" }
                }
            }

            onProgress(SessionProgress.Creating)
            sessionId = packageInstaller.createSession(params)
            Logger.d { "Created session: $sessionId" }

            val session = packageInstaller.openSession(sessionId)

            val fileSize = apkFile.length()
            var totalWritten = 0L
            var lastEmittedProgress = 0

            session.openWrite("base.apk", 0, fileSize).use { outputStream ->
                FileInputStream(apkFile).use { inputStream ->
                    val buffer = ByteArray(65536)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        totalWritten += read

                        val progress = ((totalWritten.toFloat() / fileSize) * 100).toInt()
                        if (progress >= lastEmittedProgress + 5) {
                            onProgress(SessionProgress.Writing(progress))
                            lastEmittedProgress = progress
                        }
                    }

                    outputStream.flush()
                }
            }

            onProgress(SessionProgress.Writing(100))
            session.fsync(session.openWrite("base.apk", 0, 0))

            onProgress(SessionProgress.Committing)
            val result = commitSessionAndWait(session, sessionId, apkFile)

            result

        } catch (e: Exception) {
            if (sessionId != -1) {
                try {
                    packageInstaller.abandonSession(sessionId)
                } catch (abandonError: Exception) {
                    Logger.w(abandonError) { "Failed to abandon session" }
                }
            }

            InstallResult(
                success = false,
                packageName = null,
                errorMessage = e.message
            )
        }
    }

    private suspend fun commitSessionAndWait(
        session: PackageInstaller.Session,
        sessionId: Int,
        apkFile: File
    ): InstallResult = suspendCancellableCoroutine { continuation ->

        val intent = Intent(context, InstallResultReceiver::class.java).apply {
            action = INSTALL_ACTION
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            flags
        )

        installCallbacks[sessionId] = { success, packageName, errorMessage ->
            val result = InstallResult(success, packageName, errorMessage)
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        continuation.invokeOnCancellation {
            try {
                session.abandon()
            } catch (e: Exception) {
                Logger.w(e) { "Failed to abandon session on cancellation" }
            }
            installCallbacks.remove(sessionId)
        }

        try {
            session.commit(pendingIntent.intentSender)
            Logger.d { "Session $sessionId committed" }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to commit session" }
            installCallbacks.remove(sessionId)
            if (continuation.isActive) {
                continuation.resume(
                    InstallResult(
                        success = false,
                        packageName = null,
                        errorMessage = e.message
                    )
                )
            }
        }
    }

    suspend fun uninstallPackage(packageName: String): UninstallResult {
        if (!shizukuManager.isAvailable.value || !shizukuManager.hasPermission.value) {
            return UninstallResult(false, "Shizuku not available")
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val packageInstaller = context.packageManager.packageInstaller

                val intent = Intent(context, InstallResultReceiver::class.java).apply {
                    action = UNINSTALL_ACTION
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    packageName.hashCode(),
                    intent,
                    flags
                )

                uninstallCallbacks[packageName] = { success, message ->
                    if (continuation.isActive) {
                        continuation.resume(UninstallResult(success, message))
                    }
                }

                packageInstaller.uninstall(packageName, pendingIntent.intentSender)

            } catch (e: Exception) {
                Logger.e(e) { "Failed to uninstall $packageName" }
                if (continuation.isActive) {
                    continuation.resume(UninstallResult(false, e.message ?: "Uninstall failed"))
                }
            }
        }
    }

    sealed class InstallProgress {
        object Preparing : InstallProgress()
        data class CreatingSession(val sessionId: Int) : InstallProgress()
        data class WritingApk(val progress: Int) : InstallProgress()
        object Committing : InstallProgress()
        data class Success(val packageName: String) : InstallProgress()
        data class Error(val message: String) : InstallProgress()
    }

    private sealed class SessionProgress {
        object Creating : SessionProgress()
        data class Writing(val percent: Int) : SessionProgress()
        object Committing : SessionProgress()
    }

    private data class InstallResult(
        val success: Boolean,
        val packageName: String?,
        val errorMessage: String?
    )

    data class UninstallResult(
        val success: Boolean,
        val message: String
    )

    companion object {
        const val INSTALL_ACTION = "com.github.store.INSTALL_RESULT"
        const val UNINSTALL_ACTION = "com.github.store.UNINSTALL_RESULT"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PACKAGE_NAME = "package_name"

        internal val installCallbacks = mutableMapOf<Int, (Boolean, String?, String?) -> Unit>()
        internal val uninstallCallbacks = mutableMapOf<String, (Boolean, String) -> Unit>()

        internal fun onInstallResult(
            sessionId: Int,
            success: Boolean,
            packageName: String?,
            errorMessage: String?
        ) {
            installCallbacks.remove(sessionId)?.invoke(success, packageName, errorMessage)
        }

        internal fun onUninstallResult(packageName: String, success: Boolean, message: String) {
            uninstallCallbacks.remove(packageName)?.invoke(success, message)
        }
    }
}