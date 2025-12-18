package com.example.musicplayer.database

import androidx.room.TypeConverter
import com.example.musicplayer.database.entity.LoginType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromLoginType(value: LoginType): String {
        return value.name
    }

    @TypeConverter
    fun toLoginType(value: String): LoginType {
        return LoginType.valueOf(value)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
