package com.example.musicplayer.core.extensions

import android.view.View

/**
 * View 가시성 설정 확장함수
 */
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.setVisibleOrGone(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.setVisibleOrInvisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

/**
 * 클릭 리스너 설정 (중복 클릭 방지)
 */
fun View.setOnSingleClickListener(
    debounceTime: Long = 500L,
    action: (View) -> Unit
) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            action(view)
        }
    }
}
