package com.example.musicplayer.manager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val downloadId: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class FileDownloadManager {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun downloadFile(
        context: Context,
        downloadId: String,
        url: String,
        fileName: String
    ) {
        coroutineScope.launch {
            try {
                _isDownloading.value = true
                
                // 초기 진행률 설정
                updateProgress(downloadId, 0, 0, 0)
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    updateProgress(downloadId, 0, 0, 0, false, true, "다운로드 실패: ${response.code}")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    updateProgress(downloadId, 0, 0, 0, false, true, "응답 본문이 없습니다")
                    return@launch
                }

                val contentLength = body.contentLength()
                
                // 저장할 파일 경로 생성
                val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                
                val outputFile = File(downloadDir, fileName)
                
                saveFileWithProgress(body, outputFile, downloadId, contentLength)
                
            } catch (e: Exception) {
                LogManager.e("Download failed for $downloadId: ${e.message}")
                updateProgress(downloadId, 0, 0, 0, false, true, "다운로드 오류: ${e.message}")
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private suspend fun saveFileWithProgress(
        body: ResponseBody,
        outputFile: File,
        downloadId: String,
        contentLength: Long
    ) = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        
        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(outputFile)
            
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                val progress = if (contentLength > 0) {
                    ((totalBytesRead * 100) / contentLength).toInt()
                } else {
                    0
                }
                
                updateProgress(downloadId, progress, totalBytesRead, contentLength)
            }
            
            outputStream.flush()
            
            // 다운로드 완료
            updateProgress(downloadId, 100, totalBytesRead, contentLength, true)
            LogManager.i("Download completed: ${outputFile.absolutePath}")
            
        } catch (e: IOException) {
            LogManager.e("File save error: ${e.message}")
            updateProgress(downloadId, 0, 0, 0, false, true, "파일 저장 오류: ${e.message}")
            
            // 실패한 파일 삭제
            if (outputFile.exists()) {
                outputFile.delete()
            }
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun updateProgress(
        downloadId: String,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        isCompleted: Boolean = false,
        isError: Boolean = false,
        errorMessage: String? = null
    ) {
        val currentMap = _downloadProgress.value.toMutableMap()
        currentMap[downloadId] = DownloadProgress(
            downloadId = downloadId,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            isCompleted = isCompleted,
            isError = isError,
            errorMessage = errorMessage
        )
        _downloadProgress.value = currentMap
    }

    fun cancelDownload(downloadId: String) {
        val currentMap = _downloadProgress.value.toMutableMap()
        currentMap.remove(downloadId)
        _downloadProgress.value = currentMap
    }

    fun clearCompletedDownloads() {
        val currentMap = _downloadProgress.value.toMutableMap()
        currentMap.values.removeIf { it.isCompleted || it.isError }
        _downloadProgress.value = currentMap.toMap()
    }

    fun getDownloadedFiles(context: Context): List<File> {
        val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
        return if (downloadDir.exists()) {
            downloadDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun release() {
        coroutineScope.cancel()
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0)
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}