package com.example.musicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.database.entity.Favorite
import com.example.musicplayer.database.entity.History
import com.example.musicplayer.database.entity.User
import com.example.musicplayer.database.entity.Playlist
import com.example.musicplayer.database.entity.PlaylistItem
import com.example.musicplayer.database.dao.FavoriteDao
import com.example.musicplayer.database.dao.HistoryDao
import com.example.musicplayer.database.dao.UserDao
import com.example.musicplayer.database.dao.PlaylistDao

@Database(
    entities = [
        ScoreEntity::class,
        Favorite::class,
        History::class,
        User::class,
        Playlist::class,
        PlaylistItem::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create users table
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
                // Create playlists table
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

                // Create playlist_items table
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

                // Create index for playlistId in playlist_items
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_items_playlistId ON playlist_items(playlistId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new scores table with userId and songArtist columns
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

                // Copy data from old table to new table (set default userId as 'guest')
                db.execSQL("""
                    INSERT INTO scores_new (id, userId, songName, songArtist, score, timestamp)
                    SELECT id, 'guest', songName, '', score, timestamp FROM scores
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE scores")

                // Rename new table
                db.execSQL("ALTER TABLE scores_new RENAME TO scores")
            }
        }
    }
}
