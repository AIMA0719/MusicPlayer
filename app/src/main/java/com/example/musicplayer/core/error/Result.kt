package com.example.musicplayer.core.error

/**
 * 작업 결과를 나타내는 sealed class
 * Success 또는 Error 상태를 명시적으로 처리합니다.
 */
sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val exception: AppException,
        val message: String = exception.message ?: "알 수 없는 오류"
    ) : Result<Nothing>()

    object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    /**
     * 성공 시 데이터 반환, 실패 시 null 반환
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 성공 시 데이터 반환, 실패 시 기본값 반환
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * 성공 시 데이터 반환, 실패 시 예외 발생
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    /**
     * 결과 변환
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * 결과에 따른 처리
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppException) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        /**
         * 안전한 실행 - 예외 발생 시 Error로 변환
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: AppException) {
                Error(e)
            } catch (e: Exception) {
                Error(AppException.DatabaseException(e.message ?: "알 수 없는 오류", e))
            }
        }
    }
}
