package zed.rainxch.githubstore.core.data.services.installer.shizuku

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import co.touchlab.kermit.Logger

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ShizukuInstaller.INSTALL_ACTION -> {
                val status = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE
                )
                val sessionId = intent.getIntExtra(ShizukuInstaller.EXTRA_SESSION_ID, -1)

                val success = status == PackageInstaller.STATUS_SUCCESS
                val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                val errorMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

                Logger.d { "Install result for session $sessionId: success=$success, package=$packageName" }

                ShizukuInstaller.onInstallResult(sessionId, success, packageName, errorMessage)
            }

            ShizukuInstaller.UNINSTALL_ACTION -> {
                val status = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE
                )
                val packageName =
                    intent.getStringExtra(ShizukuInstaller.EXTRA_PACKAGE_NAME) ?: ""

                val success = status == PackageInstaller.STATUS_SUCCESS
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: if (success) "Uninstalled successfully" else "Uninstall failed"

                Logger.d { "Uninstall result for $packageName: success=$success" }

                ShizukuInstaller.onUninstallResult(packageName, success, message)
            }
        }
    }
}