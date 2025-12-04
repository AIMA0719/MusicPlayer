package com.example.musicplayer.database.dao

import androidx.room.*
import com.example.musicplayer.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllByUser(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun getById(userId: String, achievementId: String): AchievementEntity?

    @Query("UPDATE achievements SET progress = :progress WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun updateProgress(userId: String, achievementId: String, progress: Int)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :unlockedAt WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun unlock(userId: String, achievementId: String, unlockedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId AND isUnlocked = 1")
    suspend fun getUnlockedCount(userId: String): Int

    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 0")
    fun getLockedAchievements(userId: String): Flow<List<AchievementEntity>>
}
