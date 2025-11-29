package com.example.musicplayer.database

import androidx.room.TypeConverter
import com.example.musicplayer.database.entity.LoginType

class Converters {
    @TypeConverter
    fun fromLoginType(value: LoginType): String {
        return value.name
    }

    @TypeConverter
    fun toLoginType(value: String): LoginType {
        return LoginType.valueOf(value)
    }
}
