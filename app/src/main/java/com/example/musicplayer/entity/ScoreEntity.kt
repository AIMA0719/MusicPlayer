package com.example.musicplayer.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songName: String,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)
