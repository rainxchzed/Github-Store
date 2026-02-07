package zed.rainxch.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.data.local.db.entities.StarredRepoEntity

@Dao
interface StarredRepoDao {
    @Query("SELECT * FROM starred_repos ORDER BY starredAt DESC")
    fun getAllStarred(): Flow<List<StarredRepoEntity>>
    
    @Query("SELECT * FROM starred_repos WHERE repoId = :repoId")
    suspend fun getStarredById(repoId: Long): StarredRepoEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM starred_repos WHERE repoId = :repoId)")
    suspend fun isStarred(repoId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM starred_repos WHERE repoId = :repoId)")
    fun isStarredFlow(repoId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM starred_repos WHERE repoId = :repoId)")
    suspend fun isStarredSync(repoId: Long): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStarred(repo: StarredRepoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStarred(repos: List<StarredRepoEntity>)
    
    @Query("DELETE FROM starred_repos WHERE repoId = :repoId")
    suspend fun deleteStarredById(repoId: Long)
    
    @Query("DELETE FROM starred_repos")
    suspend fun clearAll()
    
    @Query("""
        UPDATE starred_repos 
        SET isInstalled = :installed, 
            installedPackageName = :packageName 
        WHERE repoId = :repoId
    """)
    suspend fun updateInstallStatus(
        repoId: Long,
        installed: Boolean,
        packageName: String?
    )
    
    @Query("""
        UPDATE starred_repos 
        SET latestVersion = :version,
            latestReleaseUrl = :releaseUrl,
            lastSyncedAt = :timestamp
        WHERE repoId = :repoId
    """)
    suspend fun updateLatestVersion(
        repoId: Long,
        version: String?,
        releaseUrl: String?,
        timestamp: Long
    )
    
    @Query("SELECT COUNT(*) FROM starred_repos")
    suspend fun getCount(): Int
    
    @Query("SELECT MAX(lastSyncedAt) FROM starred_repos")
    suspend fun getLastSyncTime(): Long?

    @Transaction
    suspend fun replaceAllStarred(repos: List<StarredRepoEntity>) {
        clearAll()
        insertAllStarred(repos)
    }
}