package com.example.musicplayer.domain.model

/**
 * 녹음 히스토리 도메인 모델
 */
data class RecordingHistory(
    val id: Long = 0,
    val userId: String = "guest",
    val songName: String,
    val songArtist: String = "",
    val songDuration: Long,

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
    val difficulty: ScoringDifficulty = ScoringDifficulty.NORMAL,

    // 녹음 파일
    val recordingFilePath: String,

    // 타임스탬프
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 점수 내기 난이도
 */
enum class ScoringDifficulty(
    val displayName: String,
    val pitchTolerance: Double,
    val rhythmTolerance: Double
) {
    VERY_EASY("입문", 0.3, 0.4),
    EASY("초보", 0.2, 0.3),
    NORMAL("중수", 0.15, 0.2),
    HARD("고수", 0.1, 0.15),
    VERY_HARD("초고수", 0.05, 0.1);

    companion object {
        fun fromString(value: String): ScoringDifficulty {
            return entries.find { it.name == value } ?: NORMAL
        }
    }
}
