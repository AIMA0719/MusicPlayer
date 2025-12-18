package com.example.musicplayer.data.local.database.dao

import androidx.room.*
import com.example.musicplayer.data.local.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}
