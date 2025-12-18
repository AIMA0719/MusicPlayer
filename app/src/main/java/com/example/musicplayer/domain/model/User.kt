package com.example.musicplayer.domain.model

/**
 * 사용자 도메인 모델
 */
data class User(
    val userId: String,
    val email: String?,
    val displayName: String,
    val profileImageUrl: String?,
    val loginType: LoginType,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

/**
 * 로그인 타입
 */
enum class LoginType {
    GUEST, GOOGLE, KAKAO, NAVER
}
