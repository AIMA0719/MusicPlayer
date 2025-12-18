package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Achievement
import com.example.musicplayer.domain.model.AchievementType
import kotlinx.coroutines.flow.Flow

/**
 * 도전과제 Repository 인터페이스
 */
interface AchievementRepository {
    /**
     * 모든 도전과제 조회
     */
    fun getAllAchievements(userId: String): Flow<List<Achievement>>

    /**
     * 해금된 도전과제 조회
     */
    fun getUnlockedAchievements(userId: String): Flow<List<Achievement>>

    /**
     * 특정 도전과제 조회
     */
    suspend fun getAchievement(userId: String, achievementId: String): Achievement?

    /**
     * 도전과제 진행도 업데이트
     */
    suspend fun updateProgress(userId: String, achievementId: String, progress: Int)

    /**
     * 도전과제 해금
     */
    suspend fun unlockAchievement(userId: String, achievementId: String)

    /**
     * 도전과제 초기화 (모든 타입에 대해 초기 데이터 생성)
     */
    suspend fun initializeAchievements(userId: String)

    /**
     * 해금된 도전과제 수 조회
     */
    suspend fun getUnlockedCount(userId: String): Int

    /**
     * 전체 도전과제 수 조회
     */
    fun getTotalCount(): Int
}
