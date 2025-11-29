package com.example.musicplayer.manager

import android.content.Context
import timber.log.Timber

/**
 * ## Context Manager
 * 전역적으로 Application Context를 관리하는 매니저 클래스
 *
 * ⚠️ 메모리 누수 방지: Activity Context가 아닌 Application Context만 저장
 */
object ContextManager {
    private var applicationContext: Context? = null

    /**
     * Application Context를 설정합니다.
     * @param context Context - Application Context로 변환됩니다
     */
    fun setContext(context: Context) {
        // Activity Context가 들어와도 Application Context로 변환
        this.applicationContext = context.applicationContext
        Timber.d("ContextManager initialized with Application Context")
    }

    /**
     * Application Context를 반환합니다.
     * @return Context? Application Context (null일 경우 초기화 필요)
     */
    fun getContext(): Context? {
        if (applicationContext == null) {
            Timber.w("ContextManager not initialized! Call setContext() first.")
        }
        return applicationContext
    }

    fun clearContext() {
        applicationContext = null
        Timber.d("ContextManager cleared")
    }
}
