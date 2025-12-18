package com.example.musicplayer.core.error

/**
 * 앱 전용 예외 클래스들
 * Sealed class를 사용하여 모든 예외 타입을 명시적으로 정의합니다.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 오디오 녹음 관련 예외
     */
    class AudioRecordingException(
        message: String = "오디오 녹음 중 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 잘못된 상태 예외
     */
    class InvalidStateException(
        message: String = "잘못된 상태입니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 네트워크 관련 예외
     */
    class NetworkException(
        message: String = "네트워크 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 점수 계산 관련 예외
     */
    class ScoreCalculationException(
        message: String = "점수 계산 중 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 데이터베이스 관련 예외
     */
    class DatabaseException(
        message: String = "데이터베이스 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 권한 관련 예외
     */
    class PermissionException(
        message: String = "권한이 필요합니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 파일 관련 예외
     */
    class FileException(
        message: String = "파일 처리 중 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * 인증 관련 예외
     */
    class AuthException(
        message: String = "인증 오류가 발생했습니다",
        cause: Throwable? = null
    ) : AppException(message, cause)
}
