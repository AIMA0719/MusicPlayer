package com.example.musicplayer.di

import com.example.musicplayer.database.dao.FavoriteDao
import com.example.musicplayer.database.dao.HistoryDao
import com.example.musicplayer.repository.FavoriteRepository
import com.example.musicplayer.repository.HistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteDao: FavoriteDao
    ): FavoriteRepository {
        return FavoriteRepository(favoriteDao)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: HistoryDao
    ): HistoryRepository {
        return HistoryRepository(historyDao)
    }
}
