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
    
    private val _recordingTime = MutableStateFlow(0L)
    val recordingTime: StateFlow<Long> = _recordingTime.asStateFlow()
    
    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

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
                mediaRecorder?.pause()
                LogManager.i("Recording paused")
            } catch (e: IllegalStateException) {
                LogManager.e("Failed to pause recording: ${e.message}")
                _recordingError.value = "녹음 일시정지 실패: ${e.message}"
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                LogManager.i("Recording resumed")
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
                val elapsedTime = System.currentTimeMillis() - startTime
                _recordingTime.value = elapsedTime
                delay(100) // 100ms마다 업데이트
            }
        }
    }

    private fun cleanup() {
        _isRecording.value = false
        _recordingTime.value = 0L
        
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