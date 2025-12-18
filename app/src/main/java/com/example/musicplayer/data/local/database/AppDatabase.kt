package com.example.musicplayer.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.data.local.database.dao.*
import com.example.musicplayer.data.local.database.entity.*

@Database(
    entities = [
        ScoreEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        UserEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        RecordingHistoryEntity::class,
        AchievementEntity::class,
        UserLevelEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recordingHistoryDao(): RecordingHistoryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userLevelDao(): UserLevelDao

    companion object {
        const val DATABASE_NAME = "karaoke_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        songId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        userId TEXT NOT NULL PRIMARY KEY,
                        email TEXT,
                        displayName TEXT NOT NULL,
                        profileImageUrl TEXT,
                        loginType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlists (
                        playlistId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        coverImageUrl TEXT,
                        userId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        playlistId INTEGER NOT NULL,
                        musicUri TEXT NOT NULL,
                        musicTitle TEXT NOT NULL,
                        musicArtist TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        FOREIGN KEY(playlistId) REFERENCES playlists(playlistId) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_items_playlistId ON playlist_items(playlistId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scores_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        songName TEXT NOT NULL,
                        songArtist TEXT NOT NULL DEFAULT '',
                        score INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO scores_new (id, userId, songName, songArtist, score, timestamp)
                    SELECT id, 'guest', songName, '', score, timestamp FROM scores
                """.trimIndent())

                db.execSQL("DROP TABLE scores")
                db.execSQL("ALTER TABLE scores_new RENAME TO scores")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recording_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        songName TEXT NOT NULL,
                        songArtist TEXT NOT NULL DEFAULT '',
                        songDuration INTEGER NOT NULL,
                        totalScore INTEGER NOT NULL,
                        pitchAccuracy REAL NOT NULL,
                        rhythmScore REAL NOT NULL,
                        volumeStability REAL NOT NULL,
                        durationMatch REAL NOT NULL,
                        hasVibrato INTEGER NOT NULL DEFAULT 0,
                        vibratoScore REAL NOT NULL DEFAULT 0.0,
                        difficulty TEXT NOT NULL DEFAULT 'NORMAL',
                        recordingFilePath TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        achievementId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        progress INTEGER NOT NULL DEFAULT 0,
                        maxProgress INTEGER NOT NULL DEFAULT 1,
                        unlockedAt INTEGER,
                        PRIMARY KEY(achievementId, userId)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_level (
                        userId TEXT NOT NULL PRIMARY KEY,
                        level INTEGER NOT NULL DEFAULT 1,
                        experience INTEGER NOT NULL DEFAULT 0,
                        totalRecordings INTEGER NOT NULL DEFAULT 0,
                        averageScore REAL NOT NULL DEFAULT 0.0,
                        highestScore INTEGER NOT NULL DEFAULT 0,
                        consecutiveDays INTEGER NOT NULL DEFAULT 0,
                        lastRecordingDate INTEGER NOT NULL DEFAULT 0,
                        currentTheme TEXT NOT NULL DEFAULT 'DEFAULT'
                    )
                """.trimIndent())

                db.execSQL("INSERT OR IGNORE INTO user_level (userId) VALUES ('guest')")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
