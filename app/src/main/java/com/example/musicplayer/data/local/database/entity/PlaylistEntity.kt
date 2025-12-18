package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val musicUri: String,
    val musicTitle: String,
    val musicArtist: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
