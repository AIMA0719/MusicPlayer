package com.example.musicplayer.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.database.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.musicplayer.repository.UserRepository

object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences
    private var cachedUser: User? = null
    private var userRepository: UserRepository? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        userRepository = UserRepository(context)

        // 현재 로그인된 사용자 캐시 로드
        val userId = getCurrentUserId()
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cachedUser = userRepository?.getUserById(userId)
            }
        }
    }

    fun saveCurrentUser(userId: String) {
        prefs.edit().apply {
            putString(KEY_CURRENT_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        // 사용자 정보 캐시 업데이트
        CoroutineScope(Dispatchers.IO).launch {
            cachedUser = userRepository?.getUserById(userId)
        }
    }

    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    suspend fun getCurrentUser(): User? {
        // 캐시된 사용자가 있으면 반환
        if (cachedUser != null) {
            return cachedUser
        }

        // 캐시가 없으면 DB에서 로드
        val userId = getCurrentUserId()
        if (userId != null) {
            cachedUser = userRepository?.getUserById(userId)
        }

        return cachedUser
    }

    fun getCachedUser(): User? {
        return cachedUser
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_CURRENT_USER_ID)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        cachedUser = null
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        cachedUser = null
    }
}
