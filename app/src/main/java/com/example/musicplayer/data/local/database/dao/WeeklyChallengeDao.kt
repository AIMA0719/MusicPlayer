package com.example.musicplayer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.musicplayer.data.local.database.entity.WeeklyChallengeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 주간 챌린지 DAO
 */
@Dao
interface WeeklyChallengeDao {

    /**
     * 사용자의 현재 주간 챌린지 목록 조회
     */
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId AND startDate <= :currentDate AND endDate >= :currentDate ORDER BY isCompleted ASC, currentValue DESC")
    fun getCurrentChallenges(userId: String, currentDate: String): Flow<List<WeeklyChallengeEntity>>

    /**
     * 특정 챌린지 조회
     */
    @Query("SELECT * FROM weekly_challenges WHERE id = :challengeId")
    suspend fun getChallengeById(challengeId: String): WeeklyChallengeEntity?

    /**
     * 사용자의 특정 타입 챌린지 조회
     */
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId AND type = :type AND startDate = :weekStart")
    suspend fun getChallengeByType(userId: String, type: String, weekStart: String): WeeklyChallengeEntity?

    /**
     * 챌린지 삽입/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(challenge: WeeklyChallengeEntity)

    /**
     * 여러 챌린지 삽입/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(challenges: List<WeeklyChallengeEntity>)

    /**
     * 챌린지 업데이트
     */
    @Update
    suspend fun update(challenge: WeeklyChallengeEntity)

    /**
     * 챌린지 진행도 업데이트
     */
    @Query("UPDATE weekly_challenges SET currentValue = :newValue, isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :challengeId")
    suspend fun updateProgress(challengeId: String, newValue: Int, isCompleted: Boolean, completedAt: Long?)

    /**
     * 보상 수령 처리
     */
    @Query("UPDATE weekly_challenges SET isRewardClaimed = 1, claimedAt = :claimedAt WHERE id = :challengeId")
    suspend fun claimReward(challengeId: String, claimedAt: Long)

    /**
     * 완료된 챌린지 개수 조회
     */
    @Query("SELECT COUNT(*) FROM weekly_challenges WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedChallengeCount(userId: String): Int

    /**
     * 수령하지 않은 보상이 있는 챌린지 조회
     */
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId AND isCompleted = 1 AND isRewardClaimed = 0")
    suspend fun getUnclaimedChallenges(userId: String): List<WeeklyChallengeEntity>

    /**
     * 오래된 챌린지 삭제 (4주 이전)
     */
    @Query("DELETE FROM weekly_challenges WHERE endDate < :cutoffDate")
    suspend fun deleteOldChallenges(cutoffDate: String)

    /**
     * 사용자의 모든 챌린지 히스토리 조회
     */
    @Query("SELECT * FROM weekly_challenges WHERE userId = :userId ORDER BY startDate DESC LIMIT :limit")
    suspend fun getChallengeHistory(userId: String, limit: Int = 50): List<WeeklyChallengeEntity>
}
