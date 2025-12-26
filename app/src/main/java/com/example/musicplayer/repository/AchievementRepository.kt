package com.example.musicplayer.repository

import com.example.musicplayer.database.dao.AchievementDao
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 도전과제 Repository
 * - 도전과제 데이터 접근 계층
 * - DAO 직접 접근 방지
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {

    /**
     * 사용자의 모든 도전과제 조회
     */
    fun getAllByUser(userId: String): Flow<List<AchievementEntity>> {
        return achievementDao.getAllByUser(userId)
    }

    /**
     * 특정 도전과제 조회
     */
    suspend fun getById(userId: String, achievementId: String): AchievementEntity? {
        return achievementDao.getById(userId, achievementId)
    }

    /**
     * 도전과제 진행도 업데이트
     */
    suspend fun updateProgress(userId: String, achievementId: String, progress: Int) {
        achievementDao.updateProgress(userId, achievementId, progress)
    }

    /**
     * 도전과제 해금
     */
    suspend fun unlock(userId: String, achievementId: String, unlockedAt: Long = System.currentTimeMillis()) {
        achievementDao.unlock(userId, achievementId, unlockedAt)
    }

    /**
     * 해금된 도전과제 개수
     */
    suspend fun getUnlockedCount(userId: String): Int {
        return achievementDao.getUnlockedCount(userId)
    }

    /**
     * 해금된 도전과제 목록
     */
    fun getUnlockedAchievements(userId: String): Flow<List<AchievementEntity>> {
        return achievementDao.getUnlockedAchievements(userId)
    }

    /**
     * 잠긴 도전과제 목록
     */
    fun getLockedAchievements(userId: String): Flow<List<AchievementEntity>> {
        return achievementDao.getLockedAchievements(userId)
    }

    /**
     * 도전과제 초기화 (새 사용자 또는 새 도전과제 추가 시)
     */
    suspend fun initializeAchievements(userId: String) {
        // Flow에서 현재 값 한 번만 가져오기
        val existingAchievements = achievementDao.getAllByUser(userId).first()
        val existingIds = existingAchievements.map { it.achievementId }.toSet()

        // 새로운 도전과제만 추가
        val newAchievements = Achievement.entries
            .filter { it.id !in existingIds }
            .map { achievement ->
                AchievementEntity(
                    achievementId = achievement.id,
                    userId = userId,
                    maxProgress = achievement.maxProgress
                )
            }

        if (newAchievements.isNotEmpty()) {
            achievementDao.insertAll(newAchievements)
        }
    }

    /**
     * 도전과제 삽입
     */
    suspend fun insert(achievement: AchievementEntity) {
        achievementDao.insert(achievement)
    }

    /**
     * 여러 도전과제 삽입
     */
    suspend fun insertAll(achievements: List<AchievementEntity>) {
        achievementDao.insertAll(achievements)
    }
}
