package com.example.musicplayer.di

import com.example.musicplayer.manager.FileDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileDownloadManager(): FileDownloadManager {
        return FileDownloadManager()
    }
}
