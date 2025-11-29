package com.example.musicplayer.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,
    val email: String?,
    val displayName: String,
    val profileImageUrl: String?,
    val loginType: LoginType,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

enum class LoginType {
    GOOGLE,
    GUEST
}
