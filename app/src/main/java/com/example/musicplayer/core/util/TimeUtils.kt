package com.example.musicplayer.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 시간 관련 유틸리티
 */
object TimeUtils {

    /**
     * 밀리초를 MM:SS 형식으로 변환
     */
    fun formatMillisToTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * 밀리초를 HH:MM:SS 형식으로 변환
     */
    fun formatMillisToFullTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 타임스탬프를 날짜 문자열로 변환
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 상대적 시간 표시 (예: "3일 전")
     */
    fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "방금 전"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}분 전"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}시간 전"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}일 전"
            diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}주 전"
            else -> formatTimestamp(timestamp, "yyyy.MM.dd")
        }
    }
}
