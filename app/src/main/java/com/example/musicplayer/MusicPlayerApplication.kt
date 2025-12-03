package com.example.musicplayer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.manager.GoogleAuthManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MusicPlayerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber 로깅 초기화
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // GoogleAuthManager 초기화
        GoogleAuthManager.init(this)

        // ActivityLifecycleCallbacks 등록
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                ContextManager.setActivity(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                ContextManager.clearActivity(activity)
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        Timber.d("MusicPlayerApplication initialized")
    }
}
