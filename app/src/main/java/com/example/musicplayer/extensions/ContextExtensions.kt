package com.example.musicplayer.extensions

import android.content.Context
import android.widget.Toast
import com.example.musicplayer.manager.ToastManager

/**
 * Toast 표시 (짧게)
 */
fun Context.showToast(message: String) {
    ToastManager.showToast(message)
}

/**
 * Toast 표시 (길게)
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * dp를 px로 변환
 */
fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

/**
 * px를 dp로 변환
 */
fun Context.pxToDp(px: Int): Int {
    return (px / resources.displayMetrics.density).toInt()
}
