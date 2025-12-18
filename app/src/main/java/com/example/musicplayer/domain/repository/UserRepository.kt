package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.User
import com.example.musicplayer.domain.model.LoginType
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 Repository 인터페이스
 */
interface UserRepository {
    /**
     * 사용자 정보 조회
     */
    suspend fun getUser(userId: String): User?

    /**
     * 사용자 정보 저장/업데이트
     */
    suspend fun saveUser(user: User)

    /**
     * 사용자 삭제
     */
    suspend fun deleteUser(userId: String)

    /**
     * 현재 로그인된 사용자 ID 조회
     */
    fun getCurrentUserId(): String?

    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean

    /**
     * 로그인 처리
     */
    suspend fun login(userId: String, email: String?, displayName: String, profileImageUrl: String?, loginType: LoginType)

    /**
     * 로그아웃 처리
     */
    suspend fun logout()
}
