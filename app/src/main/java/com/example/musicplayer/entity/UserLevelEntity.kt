package com.example.musicplayer.entity

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

/**
 * 레벨 시스템 헬퍼
 */
object LevelSystem {
    /**
     * 레벨별 필요 경험치
     */
    fun getRequiredExp(level: Int): Int {
        return when {
            level <= 5 -> 100 * level
            level <= 10 -> 200 * level
            level <= 20 -> 300 * level
            else -> 500 * level
        }
    }

    /**
     * 경험치 획득 계산
     */
    fun calculateExpGain(score: Int, difficulty: String): Int {
        val baseExp = score / 10 // 90점 = 9 exp
        val difficultyMultiplier = when (difficulty) {
            "VERY_EASY" -> 0.7
            "EASY" -> 0.9
            "NORMAL" -> 1.0
            "HARD" -> 1.3
            "VERY_HARD" -> 1.5
            else -> 1.0
        }
        return (baseExp * difficultyMultiplier).toInt()
    }

    /**
     * 레벨 타이틀
     */
    fun getLevelTitle(level: Int): String {
        return when {
            level < 5 -> "🎤 초보 가수"
            level < 10 -> "🎵 아마추어 가수"
            level < 20 -> "🌟 실력파 가수"
            level < 30 -> "⭐ 베테랑 가수"
            level < 40 -> "💫 프로 가수"
            level < 50 -> "👑 마스터 가수"
            else -> "🏆 레전드 가수"
        }
    }
}
