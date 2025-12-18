package com.example.musicplayer.core.util

import timber.log.Timber

/**
 * 로깅 유틸리티
 * Timber 래퍼로, 일관된 로깅 API를 제공합니다.
 */
object LogUtil {

    fun d(message: String) {
        Timber.d(message)
    }

    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    fun i(message: String) {
        Timber.i(message)
    }

    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    fun w(message: String) {
        Timber.w(message)
    }

    fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    fun e(message: String) {
        Timber.e(message)
    }

    fun e(tag: String, message: String) {
        Timber.tag(tag).e(message)
    }

    fun e(throwable: Throwable, message: String = "") {
        Timber.e(throwable, message)
    }
}
