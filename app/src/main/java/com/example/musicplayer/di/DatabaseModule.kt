package com.example.musicplayer.di

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.ScoreDao
import com.example.musicplayer.database.dao.AchievementDao
import com.example.musicplayer.database.dao.FavoriteDao
import com.example.musicplayer.database.dao.HistoryDao
import com.example.musicplayer.database.dao.PlaylistDao
import com.example.musicplayer.database.dao.RecordingHistoryDao
import com.example.musicplayer.database.dao.UserDao
import com.example.musicplayer.database.dao.UserLevelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // AppDatabase.getDatabase()와 동일한 싱글톤 인스턴스 사용
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideScoreDao(database: AppDatabase): ScoreDao {
        return database.scoreDao()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideUserLevelDao(database: AppDatabase): UserLevelDao {
        return database.userLevelDao()
    }

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }

    @Provides
    fun provideRecordingHistoryDao(database: AppDatabase): RecordingHistoryDao {
        return database.recordingHistoryDao()
    }

    @Provides
    @Singleton
    @UserPreferences
    fun provideUserPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
}

/**
 * SharedPreferences Qualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPreferences
