package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val timestamp: Long = System.currentTimeMillis()
)
