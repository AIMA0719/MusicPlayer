package com.example.musicplayer.manager

import timber.log.Timber

/**
 * ## Log Manager
 * Timber 기반 로깅 매니저
 *
 * 기존 코드와의 호환성을 유지하면서 Timber의 장점을 활용합니다.
 * - Debug 빌드: Timber.DebugTree를 통해 자동 태깅 및 소스 위치 표시
 * - Release 빌드: CrashlyticsTree 등 커스텀 트리 사용 가능
 */
object LogManager {

    /**
     * Error 레벨 로그
     */
    fun <T> e(message: T) {
        Timber.e(message.toString())
    }

    /**
     * Error 레벨 로그 (예외 포함)
     */
    fun <T> e(throwable: Throwable, message: T) {
        Timber.e(throwable, message.toString())
    }

    /**
     * Warning 레벨 로그
     */
    fun <T> w(message: T) {
        Timber.w(message.toString())
    }

    /**
     * Info 레벨 로그
     */
    fun <T> i(message: T) {
        Timber.i(message.toString())
    }

    /**
     * Debug 레벨 로그
     */
    fun <T> d(message: T) {
        Timber.d(message.toString())
    }

    /**
     * Verbose 레벨 로그
     */
    fun <T> v(message: T) {
        Timber.v(message.toString())
    }

    /**
     * 특정 태그로 로그 출력
     */
    fun tag(tag: String): Timber.Tree {
        return Timber.tag(tag)
    }
}

