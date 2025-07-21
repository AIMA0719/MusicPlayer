package com.example.musicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.database.entity.Favorite
import com.example.musicplayer.database.entity.History
import com.example.musicplayer.database.dao.FavoriteDao
import com.example.musicplayer.database.dao.HistoryDao

@Database(entities = [ScoreEntity::class, Favorite::class, History::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karaoke_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create favorites table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        songId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        recordingPath TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
