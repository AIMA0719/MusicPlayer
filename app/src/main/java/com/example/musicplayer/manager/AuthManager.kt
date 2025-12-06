package com.example.musicplayer.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.database.entity.User

object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences
    private var cachedUser: User? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveCurrentUser(userId: String) {
        prefs.edit().apply {
            putString(KEY_CURRENT_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        cachedUser = null // 캐시 초기화 (필요시 외부에서 다시 로드)
    }

    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    fun setCachedUser(user: User?) {
        cachedUser = user
    }

    fun getCachedUserSync(): User? {
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
