package com.example.musicplayer.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * 도전과제 엔티티
 */
@Entity(tableName = "achievements", primaryKeys = ["achievementId", "userId"])
data class AchievementEntity(
    val achievementId: String, // "FIRST_RECORDING", "CONSECUTIVE_3_DAYS" 등
    val userId: String = "guest",
    @ColumnInfo(defaultValue = "0")
    val isUnlocked: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val progress: Int = 0, // 현재 진행도
    @ColumnInfo(defaultValue = "1")
    val maxProgress: Int = 1, // 목표치
    val unlockedAt: Long? = null
)
