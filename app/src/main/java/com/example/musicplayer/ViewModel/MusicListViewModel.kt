package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.MusicListState
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

    private val context = application.applicationContext

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
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor = context.contentResolver.query(uri, projection, selection, null, null)

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

    private fun analyzeOriginalMusic(music: MusicFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isAnalyzing = true) }

            val pitch = analyzePitchFromWav(File(music.uri.path ?: ""))
            _state.update {
                it.copy(
                    selectedMusic = music,
                    originalPitch = listOf(pitch) ,
                    isAnalyzing = false
                )
            }
        }
    }

    private fun analyzePitchFromWav(file: File): Float {
        // TODO: 원곡 pitch 분석 알고리즘 구현
        return 440f // 임시값
    }
}
