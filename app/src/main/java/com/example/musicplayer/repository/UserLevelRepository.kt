package com.example.musicplayer.repository

import com.example.musicplayer.database.dao.UserLevelDao
import com.example.musicplayer.entity.UserLevelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 레벨 Repository
 */
@Singleton
class UserLevelRepository @Inject constructor(
    private val userLevelDao: UserLevelDao
) {

    /**
     * 사용자 레벨 조회 (Flow)
     */
    fun getByUserId(userId: String): Flow<UserLevelEntity?> {
        return userLevelDao.getByUserId(userId)
    }

    /**
     * 사용자 레벨 조회 (동기)
     */
    suspend fun getByUserIdSync(userId: String): UserLevelEntity? {
        return userLevelDao.getByUserIdSync(userId)
    }

    /**
     * 사용자 레벨 삽입
     */
    suspend fun insert(userLevel: UserLevelEntity) {
        userLevelDao.insert(userLevel)
    }

    /**
     * 레벨 및 경험치 업데이트
     */
    suspend fun updateLevel(userId: String, level: Int, experience: Int) {
        userLevelDao.updateLevel(userId, level, experience)
    }

    /**
     * 총 녹음 횟수 증가
     */
    suspend fun incrementTotalRecordings(userId: String) {
        userLevelDao.incrementTotalRecordings(userId)
    }

    /**
     * 최고 점수 업데이트
     */
    suspend fun updateHighestScore(userId: String, score: Int) {
        userLevelDao.updateHighestScore(userId, score)
    }

    /**
     * 연속 녹음 일수 업데이트
     */
    suspend fun updateConsecutiveDays(userId: String, days: Int, lastRecordingDate: Long) {
        userLevelDao.updateConsecutiveDays(userId, days, lastRecordingDate)
    }

    /**
     * 사용자 레벨 초기화 (없으면 생성)
     */
    suspend fun ensureUserLevelExists(userId: String) {
        val existing = getByUserIdSync(userId)
        if (existing == null) {
            insert(UserLevelEntity(userId = userId))
        }
    }
}
