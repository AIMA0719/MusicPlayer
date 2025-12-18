package com.example.musicplayer.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences 래퍼 클래스
 * 앱 설정 및 간단한 상태 저장을 담당합니다.
 */
@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "music_player_prefs"

        // Keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_LAST_PLAYED_SONG_ID = "last_played_song_id"
        private const val KEY_SCORING_DIFFICULTY = "scoring_difficulty"
    }

    // Auth
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    // Settings
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()

    // Playback
    var lastPlayedSongId: String?
        get() = prefs.getString(KEY_LAST_PLAYED_SONG_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_PLAYED_SONG_ID, value).apply()

    // Scoring
    var scoringDifficulty: String
        get() = prefs.getString(KEY_SCORING_DIFFICULTY, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_SCORING_DIFFICULTY, value).apply()

    /**
     * 로그인 정보 저장
     */
    fun saveLoginInfo(userId: String) {
        this.userId = userId
        this.isLoggedIn = true
    }

    /**
     * 로그아웃 - 사용자 정보 삭제
     */
    fun logout() {
        userId = null
        isLoggedIn = false
    }

    /**
     * 모든 설정 초기화
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
