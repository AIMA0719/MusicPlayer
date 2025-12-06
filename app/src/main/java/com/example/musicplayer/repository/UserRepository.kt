package com.example.musicplayer.repository

import android.content.SharedPreferences
import com.example.musicplayer.database.dao.UserDao
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.database.entity.User
import com.example.musicplayer.di.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    @UserPreferences private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_GUEST_USER_ID = "guest_user_id"
    }

    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()
    }

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun updateLastLogin(userId: String) {
        userDao.updateLastLogin(userId)
    }

    suspend fun createGuestUser(): User {
        // 이미 저장된 게스트 ID가 있는지 확인
        val savedGuestId = prefs.getString(KEY_GUEST_USER_ID, null)

        if (savedGuestId != null) {
            // 기존 게스트 ID가 있으면 해당 사용자 조회
            val existingUser = getUserById(savedGuestId)
            if (existingUser != null) {
                // 기존 게스트 사용자가 DB에 있으면 반환
                return existingUser
            }
        }

        // 새로운 게스트 사용자 생성
        val guestUserId = "guest"
        val guestUser = User(
            userId = guestUserId,
            email = null,
            displayName = "게스트",
            profileImageUrl = null,
            loginType = LoginType.GUEST
        )

        // DB에 저장
        insertUser(guestUser)

        // SharedPreferences에 게스트 ID 저장 (앱 캐시 지우기 전까지 유지)
        prefs.edit().putString(KEY_GUEST_USER_ID, guestUserId).apply()

        return guestUser
    }

    suspend fun createGoogleUser(
        userId: String,
        email: String?,
        displayName: String,
        profileImageUrl: String?
    ): User {
        val googleUser = User(
            userId = userId,
            email = email,
            displayName = displayName,
            profileImageUrl = profileImageUrl,
            loginType = LoginType.GOOGLE
        )
        insertUser(googleUser)
        return googleUser
    }

    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteUserById(userId: String) {
        userDao.deleteUserById(userId)
    }
}
