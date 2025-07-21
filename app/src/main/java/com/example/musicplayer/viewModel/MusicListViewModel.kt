package com.example.musicplayer.viewModel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
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
                _state.update { it.copy(isAnalyzing = true, analysisProgress = 0) }

                viewModelScope.launch {
                    val pitchList = analyzePitchFromMediaUri(
                        context = context,
                        uri = intent.music.uri,
                        onProgress = { progress ->
                            _state.update { it.copy(analysisProgress = progress) }
                        }
                    )

                    onIntent(
                        MusicListIntent.AnalysisCompleted(
                            music = intent.music,
                            originalPitch = pitchList
                        )
                    )
                }
            }

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

            _state.update {
                it.copy(musicFiles = musicList, isLoading = false)
            }
        }
    }

}
