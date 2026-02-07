package zed.rainxch.githubstore.core.data.local.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import zed.rainxch.githubstore.core.data.local.db.migrations.MIGRATION_1_2
import zed.rainxch.githubstore.core.data.local.db.migrations.MIGRATION_2_3

fun initDatabase(context: Context): zed.rainxch.core.data.local.db.AppDatabase {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("github_store.db")
    return Room
        .databaseBuilder<zed.rainxch.core.data.local.db.AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
        )
        .build()
}