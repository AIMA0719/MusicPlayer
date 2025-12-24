package com.example.musicplayer.data

/**
 * 노래 모드
 */
enum class SingingMode {
    PRACTICE,   // 연습 모드: 가이드 ON, 채점 OFF
    CHALLENGE;  // 도전 모드: 가이드 OFF, 채점 ON

    val isGuideEnabled: Boolean
        get() = this == PRACTICE

    val isScoringEnabled: Boolean
        get() = this == CHALLENGE

    val displayName: String
        get() = when (this) {
            PRACTICE -> "연습 모드"
            CHALLENGE -> "도전 모드"
        }

    val description: String
        get() = when (this) {
            PRACTICE -> "원곡을 들으며 연습해보세요"
            CHALLENGE -> "점수를 획득하세요"
        }
}
