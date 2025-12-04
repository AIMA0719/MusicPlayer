package com.example.musicplayer.entity

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

/**
 * 도전과제 정의
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val maxProgress: Int,
    val icon: String // 이모지
) {
    // 기본 도전과제
    FIRST_RECORDING("FIRST_RECORDING", "첫 녹음", "첫 번째 녹음을 완료하세요", 1, "🎤"),
    FIRST_90_SCORE("FIRST_90_SCORE", "90점 돌파", "90점 이상을 달성하세요", 1, "🌟"),

    // 연속 도전과제
    CONSECUTIVE_3_DAYS("CONSECUTIVE_3_DAYS", "3일 연속 녹음", "3일 연속으로 녹음하세요", 3, "🔥"),
    CONSECUTIVE_7_DAYS("CONSECUTIVE_7_DAYS", "일주일 챌린지", "7일 연속으로 녹음하세요", 7, "💪"),

    // 점수 도전과제
    SCORE_90_5_SONGS("SCORE_90_5_SONGS", "고득점 마스터", "90점 이상을 5곡 달성하세요", 5, "⭐"),
    PERFECT_SCORE("PERFECT_SCORE", "완벽한 점수", "100점을 달성하세요", 1, "👑"),

    // 난이도 도전과제
    TRY_ALL_DIFFICULTY("TRY_ALL_DIFFICULTY", "난이도 마스터", "모든 난이도를 시도해보세요", 5, "🎯"),

    // 특수 도전과제
    VIBRATO_MASTER("VIBRATO_MASTER", "비브라토 마스터", "비브라토를 10번 성공시키세요", 10, "🎵"),
    SONG_MASTER("SONG_MASTER", "곡 정복자", "같은 곡을 10번 녹음하세요", 10, "🎼"),

    // 횟수 도전과제
    RECORDING_10("RECORDING_10", "노래방 단골", "총 10곡을 녹음하세요", 10, "🎙️"),
    RECORDING_50("RECORDING_50", "열정 가수", "총 50곡을 녹음하세요", 50, "🎶"),
    RECORDING_100("RECORDING_100", "프로 가수", "총 100곡을 녹음하세요", 100, "🏆")
}
