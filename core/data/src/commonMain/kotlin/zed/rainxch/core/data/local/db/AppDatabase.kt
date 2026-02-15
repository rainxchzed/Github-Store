package zed.rainxch.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import zed.rainxch.core.data.local.db.dao.FavoriteRepoDao
import zed.rainxch.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.core.data.local.db.dao.StarredRepoDao
import zed.rainxch.core.data.local.db.dao.UpdateHistoryDao
import zed.rainxch.core.data.local.db.entities.FavoriteRepoEntity
import zed.rainxch.core.data.local.db.entities.InstalledAppEntity
import zed.rainxch.core.data.local.db.entities.StarredRepositoryEntity
import zed.rainxch.core.data.local.db.entities.UpdateHistoryEntity

@Database(
    entities = [
        InstalledAppEntity::class,
        FavoriteRepoEntity::class,
        UpdateHistoryEntity::class,
        StarredRepositoryEntity::class,
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val installedAppDao: InstalledAppDao
    abstract val favoriteRepoDao: FavoriteRepoDao
    abstract val updateHistoryDao: UpdateHistoryDao
    abstract val starredReposDao: StarredRepoDao
}