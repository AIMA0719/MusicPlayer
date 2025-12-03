package com.example.musicplayer.manager

import android.app.Activity
import android.content.Context
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * ## Context Manager
 * 전역적으로 현재 활성화된 Activity Context를 관리하는 매니저 클래스
 *
 * ⚠️ 메모리 누수 방지: WeakReference를 사용하여 Activity를 저장
 */
object ContextManager {
    private var currentActivity: WeakReference<Activity>? = null

    /**
     * 현재 활성화된 Activity Context를 설정합니다.
     * @param activity Activity - 현재 활성화된 Activity
     */
    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
        Timber.d("Current activity set: ${activity.javaClass.simpleName}")
    }

    /**
     * 현재 활성화된 Activity Context를 반환합니다.
     * @return Activity? 현재 활성화된 Activity (null일 경우 사용 불가)
     */
    fun getActivity(): Activity? {
        val activity = currentActivity?.get()
        if (activity == null) {
            Timber.w("Current activity is null or has been destroyed.")
        }
        return activity
    }

    fun clearActivity(activity: Activity) {
        if (currentActivity?.get() == activity) {
            currentActivity?.clear()
            Timber.d("Current activity cleared: ${activity.javaClass.simpleName}")
        }
    }
}
