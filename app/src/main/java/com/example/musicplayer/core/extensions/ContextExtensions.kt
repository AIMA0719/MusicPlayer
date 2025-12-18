package com.example.musicplayer.core.extensions

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Toast 표시 확장함수
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, duration).show()
}

/**
 * dp를 px로 변환
 */
fun Context.dpToPx(dp: Float): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.dpToPx(dp: Int): Int {
    return dpToPx(dp.toFloat())
}

/**
 * px를 dp로 변환
 */
fun Context.pxToDp(px: Int): Float {
    return px / resources.displayMetrics.density
}
