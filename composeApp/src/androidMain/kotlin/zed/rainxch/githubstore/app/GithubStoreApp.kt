package zed.rainxch.githubstore.app

import android.app.Application
import android.os.Build
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import zed.rainxch.core.data.services.PackageEventReceiver
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.system.PackageMonitor
import zed.rainxch.githubstore.app.di.initKoin

class GithubStoreApp : Application() {

    private var packageEventReceiver: PackageEventReceiver? = null

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@GithubStoreApp)
        }

        registerPackageEventReceiver()
    }

    private fun registerPackageEventReceiver() {
        val receiver = PackageEventReceiver(
            installedAppsRepository = get<InstalledAppsRepository>(),
            packageMonitor = get<PackageMonitor>()
        )
        val filter = PackageEventReceiver.createIntentFilter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        packageEventReceiver = receiver
    }
}