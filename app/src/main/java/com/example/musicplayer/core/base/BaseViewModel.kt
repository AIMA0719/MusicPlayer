package com.example.musicplayer.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel의 기본 클래스
 * 공통 에러 처리 및 코루틴 지원을 제공합니다.
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * 에러 핸들러 - 서브클래스에서 오버라이드 가능
     */
    protected open val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleException(throwable)
    }

    /**
     * 예외 처리 - 서브클래스에서 오버라이드 가능
     */
    protected open fun handleException(throwable: Throwable) {
        // 기본 구현: 로깅
        throwable.printStackTrace()
    }

    /**
     * 안전한 코루틴 실행
     */
    protected fun safeLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }
}
