package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.Manager.LogManager
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.MusicListState
import com.example.musicplayer.factory.MusicFileDispatcherFactory.analyzePitchFromWavInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream

class MusicListViewModel(
    application: Application
) : AndroidViewModel(application) {

    val context: Context by lazy { application.applicationContext }

    private val _state = MutableStateFlow(MusicListState())
    val state: StateFlow<MusicListState> = _state.asStateFlow()

    fun onIntent(intent: MusicListIntent) {
        when (intent) {
            is MusicListIntent.LoadMusicFiles -> loadMusicFiles()
            is MusicListIntent.AnalyzeOriginalMusic -> analyzeOriginalMusic(intent.music)
            is MusicListIntent.AnalysisCompleted -> _state.update {
                it.copy(
                    selectedMusic = intent.music,
                    originalPitch = intent.originalPitch,
                    isAnalyzing = false
                )
            }
        }
    }

    private fun loadMusicFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }

            val musicList = mutableListOf<MusicFile>()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.MIME_TYPE
            )

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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

            _state.update {
                it.copy(musicFiles = musicList, isLoading = false)
            }
        }
    }

    /*private fun analyzeOriginalMusic(music: MusicFile) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }

            val inputStream = context.contentResolver.openInputStream(music.uri)

            val pitchList = inputStream?.let { input ->
                analyzePitchFromInputStream(
                    inputStream = input,
                    durationInMillis = music.duration,
                    onProgress = { progress ->
                        _state.update { it.copy(analysisProgress = progress) }
                    }
                )
            } ?: emptyList()

            _state.update {
                it.copy(
                    selectedMusic = music,
                    originalPitch = pitchList,
                    isAnalyzing = false,
                    analysisProgress = 100
                )
            }
        }
    }*/

    private fun analyzeOriginalMusic(music: MusicFile) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analysisProgress = 0) }

            val descriptor = context.contentResolver.openFileDescriptor(music.uri, "r")
            val fileLength = descriptor?.statSize ?: 0L
            val inputStream = descriptor?.fileDescriptor?.let { FileInputStream(it) }

            val pitchList = inputStream?.let { input ->
                analyzePitchFromWavInputStream(
                    inputStream = input,
                    fileLengthBytes = fileLength,
                    onProgress = { progress ->
                        _state.update { it.copy(analysisProgress = progress) }
                    }
                )
            } ?: emptyList()

            _state.update {
                it.copy(
                    selectedMusic = music,
                    originalPitch = pitchList,
                    isAnalyzing = false,
                    analysisProgress = 100
                )
            }
        }
    }


}
