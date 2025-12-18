package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val score: Int,
    val recordingPath: String,
    val timestamp: Long = System.currentTimeMillis()
)
