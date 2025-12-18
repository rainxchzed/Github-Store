package zed.rainxch.githubstore.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import zed.rainxch.githubstore.app.di.initKoin

class GithubStoreApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@GithubStoreApp)
        }
    }
}