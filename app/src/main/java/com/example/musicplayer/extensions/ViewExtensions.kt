package com.example.musicplayer.extensions

import android.view.View
import androidx.core.view.isVisible

/**
 * View 표시
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * View 숨김 (INVISIBLE)
 */
fun View.hide() {
    visibility = View.INVISIBLE
}

/**
 * View 제거 (GONE)
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * View 표시/숨김 토글
 */
fun View.toggleVisibility() {
    visibility = if (isVisible) View.GONE else View.VISIBLE
}

/**
 * 조건에 따라 View 표시/제거
 */
fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * 여러 View를 한번에 표시
 */
fun showViews(vararg views: View) {
    views.forEach { it.show() }
}

/**
 * 여러 View를 한번에 제거
 */
fun goneViews(vararg views: View) {
    views.forEach { it.gone() }
}
