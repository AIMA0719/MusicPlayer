package com.example.musicplayer.domain.model

/**
 * 점수 도메인 모델
 */
data class Score(
    val id: Int = 0,
    val userId: String,
    val songName: String,
    val songArtist: String = "",
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 점수 분포 통계
 */
data class ScoreDistribution(
    val range: String,
    val count: Int
)

/**
 * 일별 녹음 횟수
 */
data class DailyRecordingCount(
    val date: String,
    val count: Int
)
