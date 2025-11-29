package com.example.musicplayer

import android.app.Application
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

        Timber.d("MusicPlayerApplication initialized")
    }
}
