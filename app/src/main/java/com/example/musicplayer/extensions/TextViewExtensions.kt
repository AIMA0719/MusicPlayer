package com.example.musicplayer.extensions

import android.widget.TextView

/**
 * TextView 텍스트 설정 (null-safe)
 */
fun TextView.setTextOrEmpty(text: String?) {
    this.text = text ?: ""
}

/**
 * TextView 텍스트 설정 (기본값 지정)
 */
fun TextView.setTextOrDefault(text: String?, default: String = "-") {
    this.text = text ?: default
}

/**
 * 숫자를 점수 형식으로 표시 (예: "95점")
 */
fun TextView.setScore(score: Int) {
    text = "${score}점"
}

/**
 * 레벨 형식으로 표시 (예: "Lv.5")
 */
fun TextView.setLevel(level: Int) {
    text = "Lv.$level"
}
