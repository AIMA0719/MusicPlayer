package com.example.musicplayer.Manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class RecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    var isRecording: Boolean = false
        private set

    /**
     * 시작 버튼을 누르면 호출되는 함수로 녹음을 시작합니다.
     */
    fun startRecording() {
        if (isRecording) return // 이미 녹음 중이면 무시

        if (ContextCompat.checkSelfPermission(ContextManager.mainContext!!,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한 요청
            ActivityCompat.requestPermissions(
                ContextManager.mainActivity!!,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }else{
            try {
                outputFilePath = generateOutputFilePath()
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC) // 또는 DEFAULT
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(outputFilePath)
                    prepare()
                    start()
                }
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
                stopRecording() // 오류 발생 시 녹음 종료
            } catch (e: RuntimeException) {
                e.printStackTrace()
                Toast.makeText(context, "녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
                stopRecording()
            }
        }

    }


    /**
     * 중지 버튼을 누르면 호출되는 함수로 녹음을 종료합니다.
     */
    fun stopRecording() {
        if (!isRecording) return // 녹음 중이 아니면 무시

        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
    }

    /**
     * 다시 시작을 위한 파일 경로를 반환합니다.
     */
    fun getRecordedFilePath(): String? {
        return outputFilePath
    }

    /**
     * 임시 녹음 파일 경로를 생성하는 함수
     */
    private fun generateOutputFilePath(): String {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File(outputDir, "recorded_audio_${System.currentTimeMillis()}.3gp").absolutePath
    }
}
