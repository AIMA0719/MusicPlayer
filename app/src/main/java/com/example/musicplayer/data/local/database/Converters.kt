package com.example.musicplayer.data.local.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room 데이터베이스 타입 변환기
 */
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

/**
 * 로그인 타입
 */
enum class LoginType {
    GUEST,
    GOOGLE,
    KAKAO,
    NAVER
}
