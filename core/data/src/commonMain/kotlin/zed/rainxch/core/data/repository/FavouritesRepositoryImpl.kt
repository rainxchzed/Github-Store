package zed.rainxch.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import zed.rainxch.githubstore.core.data.local.db.dao.FavoriteRepoDao
import zed.rainxch.githubstore.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.githubstore.core.data.local.db.entities.FavoriteRepo
import zed.rainxch.githubstore.core.domain.repository.FavouritesRepository
import zed.rainxch.githubstore.feature.details.domain.repository.DetailsRepository

class FavouritesRepositoryImpl(
    private val dao: FavoriteRepoDao,
    private val installedAppsDao: InstalledAppDao,
    private val detailsRepository: DetailsRepository
) : FavouritesRepository {
    
    override fun getAllFavorites(): Flow<List<FavoriteRepo>> = dao.getAllFavorites()
    
    override fun isFavorite(repoId: Long): Flow<Boolean> = dao.isFavorite(repoId)
    
    override suspend fun isFavoriteSync(repoId: Long): Boolean = dao.isFavoriteSync(repoId)
    
    override suspend fun addFavorite(repo: FavoriteRepo) {

        val installedApp = installedAppsDao.getAppByRepoId(repo.repoId)
        dao.insertFavorite(
            repo.copy(
                isInstalled = installedApp != null,
                installedPackageName = installedApp?.packageName
            )
        )
    }
    
    override suspend fun removeFavorite(repoId: Long) {
        dao.deleteFavoriteById(repoId)
    }
    
    override suspend fun toggleFavorite(repo: FavoriteRepo) {
        if (dao.isFavoriteSync(repo.repoId)) {
            removeFavorite(repo.repoId)
        } else {
            addFavorite(repo)
        }
    }
    
    override suspend fun updateFavoriteInstallStatus(
        repoId: Long,
        installed: Boolean,
        packageName: String?
    ) {
        dao.updateInstallStatus(repoId, installed, packageName)
    }
    
    override suspend fun syncFavoriteVersions() {
        val favorites = dao.getAllFavorites().first()
        favorites.forEach { favorite ->
            try {
                val latestRelease = detailsRepository.getLatestPublishedRelease(
                    owner = favorite.repoOwner,
                    repo = favorite.repoName,
                    defaultBranch = ""
                )
                
                dao.updateLatestVersion(
                    repoId = favorite.repoId,
                    version = latestRelease?.tagName,
                    releaseUrl = latestRelease?.htmlUrl,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {

            }
        }
    }
}