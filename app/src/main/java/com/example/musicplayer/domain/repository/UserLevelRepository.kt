package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.UserLevel
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 레벨 Repository 인터페이스
 */
interface UserLevelRepository {
    /**
     * 사용자 레벨 정보 조회
     */
    fun getUserLevel(userId: String): Flow<UserLevel?>

    /**
     * 사용자 레벨 정보 저장/업데이트
     */
    suspend fun saveUserLevel(userLevel: UserLevel)

    /**
     * 경험치 추가
     */
    suspend fun addExperience(userId: String, exp: Int): UserLevel

    /**
     * 레벨업 확인 (레벨업 시 새 레벨 반환, 아니면 null)
     */
    suspend fun checkLevelUp(userId: String): Int?

    /**
     * 사용자 레벨 초기화
     */
    suspend fun initializeUserLevel(userId: String)
}
