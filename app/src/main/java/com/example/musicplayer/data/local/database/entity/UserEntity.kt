package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.data.local.database.LoginType

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String?,
    val displayName: String,
    val profileImageUrl: String?,
    val loginType: LoginType,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
