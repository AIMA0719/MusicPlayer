package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val songName: String,
    val songArtist: String = "",
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)
