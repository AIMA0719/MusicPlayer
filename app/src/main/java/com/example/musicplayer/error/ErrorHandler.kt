package com.example.musicplayer.error

import android.content.Context
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager

object ErrorHandler {
    fun handleException(context: Context, exception: Throwable) {
        when (exception) {
            is AppException -> handleAppException(context, exception)
            else -> handleGenericException(context, exception)
        }
    }

    private fun handleAppException(context: Context, exception: AppException) {
        when (exception) {
            is AppException.AudioRecordingException -> {
                LogManager.e("Audio recording error: ${exception.message}")
                ToastManager.showToast("녹음 중 오류가 발생했습니다: ${exception.message}")
            }
            is AppException.ScoreCalculationException -> {
                LogManager.e("Score calculation error: ${exception.message}")
                ToastManager.showToast("점수 계산 중 오류가 발생했습니다: ${exception.message}")
            }
            is AppException.ResourceInitializationException -> {
                LogManager.e("Resource initialization error: ${exception.message}")
                ToastManager.showToast("리소스 초기화 중 오류가 발생했습니다: ${exception.message}")
            }
            is AppException.InvalidStateException -> {
                LogManager.e("Invalid state error: ${exception.message}")
                ToastManager.showToast("잘못된 상태입니다: ${exception.message}")
            }
        }
    }

    private fun handleGenericException(context: Context, exception: Throwable) {
        LogManager.e("Unexpected error: ${exception.message}")
        ToastManager.showToast("예기치 않은 오류가 발생했습니다: ${exception.message}")
    }
} 