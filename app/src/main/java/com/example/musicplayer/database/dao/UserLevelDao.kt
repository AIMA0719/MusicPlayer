package com.example.musicplayer.database.dao

import androidx.room.*
import com.example.musicplayer.entity.UserLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLevelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userLevel: UserLevelEntity)

    @Query("SELECT * FROM user_level WHERE userId = :userId")
    fun getByUserId(userId: String): Flow<UserLevelEntity?>

    @Query("SELECT * FROM user_level WHERE userId = :userId")
    suspend fun getByUserIdSync(userId: String): UserLevelEntity?

    @Update
    suspend fun update(userLevel: UserLevelEntity)

    @Query("UPDATE user_level SET experience = experience + :exp WHERE userId = :userId")
    suspend fun addExperience(userId: String, exp: Int)

    @Query("UPDATE user_level SET level = :level, experience = :experience WHERE userId = :userId")
    suspend fun updateLevel(userId: String, level: Int, experience: Int)

    @Query("UPDATE user_level SET totalRecordings = totalRecordings + 1 WHERE userId = :userId")
    suspend fun incrementTotalRecordings(userId: String)

    @Query("UPDATE user_level SET highestScore = :score WHERE userId = :userId AND :score > highestScore")
    suspend fun updateHighestScore(userId: String, score: Int)

    @Query("UPDATE user_level SET currentTheme = :theme WHERE userId = :userId")
    suspend fun updateTheme(userId: String, theme: String)

    @Query("UPDATE user_level SET consecutiveDays = :days, lastRecordingDate = :date WHERE userId = :userId")
    suspend fun updateConsecutiveDays(userId: String, days: Int, date: Long)
}
