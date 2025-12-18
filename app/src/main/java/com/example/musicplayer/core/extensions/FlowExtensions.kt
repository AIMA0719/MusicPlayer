package com.example.musicplayer.core.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.musicplayer.core.error.Result
import com.example.musicplayer.core.error.AppException

/**
 * Flow를 Result로 변환
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .catch { e ->
        val appException = when (e) {
            is AppException -> e
            else -> AppException.DatabaseException(e.message ?: "알 수 없는 오류", e)
        }
        emit(Result.Error(appException))
    }

/**
 * Fragment/Activity에서 Flow를 안전하게 수집
 */
fun <T> Flow<T>.collectWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { collector(it) }
        }
    }
}
