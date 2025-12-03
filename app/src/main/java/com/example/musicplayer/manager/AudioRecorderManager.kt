package com.example.musicplayer.manager

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderManager {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    private var startTime: Long = 0
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingTime = MutableStateFlow(0L)
    val recordingTime: StateFlow<Long> = _recordingTime.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private var pausedTime: Long = 0
    private var pauseStartTime: Long = 0

    fun startRecording(context: Context): String? {
        try {
            if (_isRecording.value) {
                LogManager.w("Recording is already in progress")
                return null
            }

            val outputFile = createOutputFile(context)
            outputFilePath = outputFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFilePath)
                
                try {
                    prepare()
                    start()
                } catch (e: IOException) {
                    LogManager.e("Failed to start recording: ${e.message}")
                    _recordingError.value = "녹음 시작 실패: ${e.message}"
                    cleanup()
                    return null
                }
            }

            _isRecording.value = true
            startTime = System.currentTimeMillis()
            startTimer()
            
            LogManager.i("Recording started: $outputFilePath")
            return outputFilePath
            
        } catch (e: Exception) {
            LogManager.e("Failed to initialize recording: ${e.message}")
            _recordingError.value = "녹음 초기화 실패: ${e.message}"
            cleanup()
            return null
        }
    }

    fun stopRecording(): String? {
        try {
            if (!_isRecording.value) {
                LogManager.w("No recording in progress")
                return null
            }

            mediaRecorder?.apply {
                try {
                    stop()
                    LogManager.i("Recording stopped: $outputFilePath")
                } catch (e: RuntimeException) {
                    LogManager.e("Failed to stop recording: ${e.message}")
                    _recordingError.value = "녹음 정지 실패: ${e.message}"
                }
            }

            val savedFilePath = outputFilePath
            cleanup()
            
            return savedFilePath
            
        } catch (e: Exception) {
            LogManager.e("Error stopping recording: ${e.message}")
            _recordingError.value = "녹음 종료 중 오류: ${e.message}"
            cleanup()
            return null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (!_isPaused.value && _isRecording.value) {
                    mediaRecorder?.pause()
                    _isPaused.value = true
                    pauseStartTime = System.currentTimeMillis()
                    LogManager.i("Recording paused")
                }
            } catch (e: IllegalStateException) {
                LogManager.e("Failed to pause recording: ${e.message}")
                _recordingError.value = "녹음 일시정지 실패: ${e.message}"
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (_isPaused.value && _isRecording.value) {
                    mediaRecorder?.resume()
                    _isPaused.value = false
                    // 일시정지된 시간 누적
                    pausedTime += System.currentTimeMillis() - pauseStartTime
                    LogManager.i("Recording resumed")
                }
            } catch (e: IllegalStateException) {
                LogManager.e("Failed to resume recording: ${e.message}")
                _recordingError.value = "녹음 재개 실패: ${e.message}"
            }
        }
    }

    private fun createOutputFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timestamp.m4a"
        
        // 앱의 외부 저장소 디렉토리 사용 (Android/data/package/files/Recordings)
        val recordingsDir = File(context.getExternalFilesDir(null), "Recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        
        return File(recordingsDir, fileName)
    }

    private fun startTimer() {
        coroutineScope.launch {
            while (_isRecording.value) {
                if (!_isPaused.value) {
                    // 일시정지 중이 아닐 때만 시간 업데이트
                    val elapsedTime = System.currentTimeMillis() - startTime - pausedTime
                    _recordingTime.value = elapsedTime

                    // 진폭 업데이트
                    try {
                        val currentAmplitude = mediaRecorder?.maxAmplitude ?: 0
                        _amplitude.value = currentAmplitude
                    } catch (e: Exception) {
                        LogManager.e("Failed to get amplitude: ${e.message}")
                    }
                }
                delay(100) // 100ms마다 업데이트
            }
        }
    }

    private fun cleanup() {
        _isRecording.value = false
        _isPaused.value = false
        _recordingTime.value = 0L
        _amplitude.value = 0
        pausedTime = 0L
        pauseStartTime = 0L

        mediaRecorder?.apply {
            try {
                reset()
                release()
            } catch (e: Exception) {
                LogManager.e("Error during recorder cleanup: ${e.message}")
            }
        }
        mediaRecorder = null
        outputFilePath = null
    }

    fun release() {
        if (_isRecording.value) {
            stopRecording()
        }
        coroutineScope.cancel()
    }

    fun clearError() {
        _recordingError.value = null
    }

    fun getRecordingFile(): File? {
        return outputFilePath?.let { File(it) }
    }

    companion object {
        fun formatTime(milliseconds: Long): String {
            val seconds = (milliseconds / 1000) % 60
            val minutes = (milliseconds / (1000 * 60)) % 60
            val hours = (milliseconds / (1000 * 60 * 60))
            
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }
}