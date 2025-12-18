package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 레벨 엔티티
 */
@Entity(tableName = "user_level")
data class UserLevelEntity(
    @PrimaryKey val userId: String = "guest",
    val level: Int = 1,
    val experience: Int = 0, // 현재 경험치
    val totalRecordings: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Int = 0,
    val consecutiveDays: Int = 0,
    val lastRecordingDate: Long = 0L,
    val currentTheme: String = "DEFAULT" // DEFAULT, KARAOKE, CONCERT, DARK
)
