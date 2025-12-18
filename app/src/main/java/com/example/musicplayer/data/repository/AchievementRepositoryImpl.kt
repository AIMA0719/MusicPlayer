package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.AchievementDao
import com.example.musicplayer.data.local.database.entity.AchievementEntity
import com.example.musicplayer.domain.model.Achievement
import com.example.musicplayer.domain.model.AchievementType
import com.example.musicplayer.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    override fun getAllAchievements(userId: String): Flow<List<Achievement>> {
        return achievementDao.getAllByUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUnlockedAchievements(userId: String): Flow<List<Achievement>> {
        return achievementDao.getUnlockedAchievements(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAchievement(userId: String, achievementId: String): Achievement? {
        return achievementDao.getById(userId, achievementId)?.toDomain()
    }

    override suspend fun updateProgress(userId: String, achievementId: String, progress: Int) {
        achievementDao.updateProgress(userId, achievementId, progress)
    }

    override suspend fun unlockAchievement(userId: String, achievementId: String) {
        achievementDao.unlock(userId, achievementId)
    }

    override suspend fun initializeAchievements(userId: String) {
        val entities = AchievementType.entries.map { type ->
            AchievementEntity(
                achievementId = type.id,
                userId = userId,
                isUnlocked = false,
                progress = 0,
                maxProgress = type.maxProgress,
                unlockedAt = null
            )
        }
        achievementDao.insertAll(entities)
    }

    override suspend fun getUnlockedCount(userId: String): Int {
        return achievementDao.getUnlockedCount(userId)
    }

    override fun getTotalCount(): Int {
        return AchievementType.entries.size
    }

    private fun AchievementEntity.toDomain(): Achievement {
        val type = AchievementType.entries.find { it.id == achievementId }
        return Achievement(
            id = achievementId,
            title = type?.title ?: "",
            description = type?.description ?: "",
            maxProgress = type?.maxProgress ?: 1,
            icon = type?.icon ?: "",
            isHidden = type?.isHidden ?: false,
            isUnlocked = isUnlocked,
            progress = progress,
            unlockedAt = unlockedAt
        )
    }
}
