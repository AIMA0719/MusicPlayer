package com.example.musicplayer.viewModel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.MusicListState
import com.example.musicplayer.factory.MusicFileDispatcherFactory.analyzePitchFromMediaUri
import com.example.musicplayer.manager.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MusicListViewModel(
    application: Application
) : AndroidViewModel(application) {

    val context: Context by lazy { application.applicationContext }

    private val _state = MutableStateFlow(MusicListState())
    val state: StateFlow<MusicListState> = _state.asStateFlow()

    fun onIntent(intent: MusicListIntent) {
        when (intent) {
            is MusicListIntent.LoadMusicFiles -> loadMusicFiles()

            is MusicListIntent.AnalyzeOriginalMusic -> {
                LogManager.i("MusicListViewModel: Starting analysis for ${intent.music.title}")
                _state.update { it.copy(isAnalyzing = true, analysisProgress = 0, hasNavigated = false) }

                viewModelScope.launch {
                    val pitchList = analyzePitchFromMediaUri(
                        context = context,
                        uri = intent.music.uri,
                        onProgress = { progress ->
                            _state.update { it.copy(analysisProgress = progress) }
                        }
                    )

                    LogManager.i("MusicListViewModel: Analysis completed, pitch list size: ${pitchList.size}")
                    onIntent(
                        MusicListIntent.AnalysisCompleted(
                            music = intent.music,
                            originalPitch = pitchList
                        )
                    )
                }
            }

            is MusicListIntent.AnalysisCompleted -> {
                LogManager.i("MusicListViewModel: AnalysisCompleted - Setting state with originalPitch and selectedMusic")
                _state.update {
                    it.copy(
                        selectedMusic = intent.music,
                        originalPitch = intent.originalPitch,
                        isAnalyzing = false
                    )
                }
            }

            is MusicListIntent.MarkAsNavigated -> {
                LogManager.i("MusicListViewModel: Marking as navigated")
                _state.update {
                    it.copy(hasNavigated = true)
                }
            }

            is MusicListIntent.ResetAnalysisState -> {
                LogManager.i("MusicListViewModel: Resetting analysis state")
                _state.update {
                    it.copy(
                        selectedMusic = null,
                        originalPitch = null,
                        isAnalyzing = false,
                        analysisProgress = 0,
                        hasNavigated = false
                    )
                }
            }
        }
    }

    private fun loadMusicFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }

            val musicList = mutableListOf<MusicFile>()

            // 1. 앱의 Recordings 디렉토리에서 녹음 파일 로드 (최우선)
            loadRecordingsFromAppDirectory(musicList)

            // 2. MediaStore에서 오디오 파일 로드
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val cursor = context.contentResolver.query(
                uri,
                null,  // projection 전부
                null,  // selection 없음
                null,  // selectionArgs 없음
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            LogManager.e("전체 오디오 파일 수: ${cursor?.count}")

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol)
                    val artist = it.getString(artistCol)
                    val duration = it.getLong(durationCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)

                    musicList.add(MusicFile(contentUri, title, artist, duration))
                }
            }

            LogManager.i("총 로드된 음악 파일 수: ${musicList.size}")

            _state.update {
                it.copy(musicFiles = musicList, isLoading = false)
            }
        }
    }

    /**
     * 앱의 Recordings 디렉토리에서 녹음 파일 로드
     */
    private fun loadRecordingsFromAppDirectory(musicList: MutableList<MusicFile>) {
        try {
            val recordingsDir = File(context.getExternalFilesDir(null), "Recordings")
            if (!recordingsDir.exists() || !recordingsDir.isDirectory) {
                LogManager.d("Recordings directory does not exist")
                return
            }

            val recordingFiles = recordingsDir.listFiles { file ->
                file.extension.lowercase() in listOf("m4a", "mp4", "mp3", "wav")
            }?.sortedByDescending { it.lastModified() } // 최신 파일 먼저

            recordingFiles?.forEach { file ->
                try {
                    val uri = file.toUri()
                    val duration = getAudioDuration(file)
                    val title = file.nameWithoutExtension
                    val artist = "내 녹음"

                    musicList.add(MusicFile(uri, title, artist, duration))
                    LogManager.d("Added recording: ${file.name}, duration: $duration ms")
                } catch (e: Exception) {
                    LogManager.e("Error loading recording file ${file.name}: ${e.message}")
                }
            }

            LogManager.i("Loaded ${recordingFiles?.size ?: 0} recordings from app directory")
        } catch (e: Exception) {
            LogManager.e("Error loading recordings: ${e.message}")
        }
    }

    /**
     * 오디오 파일의 재생 시간 가져오기
     */
    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            LogManager.e("Failed to get audio duration for ${file.name}: ${e.message}")
            0L
        }
    }

}
