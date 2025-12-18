package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 녹음 히스토리 엔티티
 * - 상세한 점수 정보 포함
 * - 녹음 파일 경로 저장
 */
@Entity(tableName = "recording_history")
data class RecordingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "guest",
    val songName: String,
    val songArtist: String = "",
    val songDuration: Long, // 밀리초

    // 점수 정보
    val totalScore: Int,
    val pitchAccuracy: Double,
    val rhythmScore: Double,
    val volumeStability: Double,
    val durationMatch: Double,

    // 비브라토 정보
    val hasVibrato: Boolean = false,
    val vibratoScore: Double = 0.0,

    // 난이도
    val difficulty: String = "NORMAL", // VERY_EASY, EASY, NORMAL, HARD, VERY_HARD

    // 녹음 파일
    val recordingFilePath: String,

    // 타임스탬프
    val timestamp: Long = System.currentTimeMillis()
)
