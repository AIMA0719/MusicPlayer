package com.example.musicplayer.viewModel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.MusicListSideEffect
import com.example.musicplayer.data.MusicListState
import com.example.musicplayer.factory.MusicFileDispatcherFactory.analyzePitchFromMediaUri
import com.example.musicplayer.manager.LogManager
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import java.io.File

class MusicListViewModel(
    application: Application
) : AndroidViewModel(application), ContainerHost<MusicListState, MusicListSideEffect> {

    val context: Context by lazy { application.applicationContext }

    override val container: Container<MusicListState, MusicListSideEffect> =
        container(MusicListState())

    // 분석 취소 플래그
    @Volatile
    private var isAnalysisCancelled = false

    fun onIntent(intent: MusicListIntent) {
        when (intent) {
            is MusicListIntent.LoadMusicFiles -> loadMusicFiles()

            is MusicListIntent.AnalyzeOriginalMusic -> analyzeOriginalMusic(intent.music)

            is MusicListIntent.AnalysisCompleted -> analysisCompleted(
                intent.music,
                intent.originalPitch
            )

            is MusicListIntent.MarkAsNavigated -> markAsNavigated()

            is MusicListIntent.ResetAnalysisState -> resetAnalysisState()

            is MusicListIntent.CancelAnalysis -> cancelAnalysis()
        }
    }

    private fun cancelAnalysis() = intent {
        LogManager.i("MusicListViewModel: Analysis cancelled by user")
        isAnalysisCancelled = true
        reduce {
            state.copy(
                isAnalyzing = false,
                analysisProgress = 0,
                selectedMusic = null,
                originalPitch = null
            )
        }
    }

    private fun analyzeOriginalMusic(music: MusicFile) = intent {
        LogManager.i("MusicListViewModel: Starting analysis for ${music.title}")
        isAnalysisCancelled = false
        reduce {
            state.copy(isAnalyzing = true, analysisProgress = 0, hasNavigated = false)
        }

        val pitchList = analyzePitchFromMediaUri(
            context = context,
            uri = music.uri,
            onProgress = { progress ->
                // 취소되었으면 진행 상황 업데이트 무시
                if (!isAnalysisCancelled) {
                    intent {
                        reduce { state.copy(analysisProgress = progress) }
                    }
                }
            }
        )

        // 취소되었으면 결과 무시
        if (isAnalysisCancelled) {
            LogManager.i("MusicListViewModel: Analysis was cancelled, ignoring results")
            return@intent
        }

        LogManager.i("MusicListViewModel: Analysis completed, pitch list size: ${pitchList.size}")
        analysisCompleted(music, pitchList)
    }

    private fun analysisCompleted(music: MusicFile, originalPitch: List<Float>) = intent {
        LogManager.i("MusicListViewModel: AnalysisCompleted - Setting state with originalPitch and selectedMusic")
        reduce {
            state.copy(
                selectedMusic = music,
                originalPitch = originalPitch,
                isAnalyzing = false
            )
        }
    }

    private fun markAsNavigated() = intent {
        LogManager.i("MusicListViewModel: Marking as navigated")
        reduce {
            state.copy(hasNavigated = true)
        }
    }

    private fun resetAnalysisState() = intent {
        LogManager.i("MusicListViewModel: Resetting analysis state")
        reduce {
            state.copy(
                selectedMusic = null,
                originalPitch = null,
                isAnalyzing = false,
                analysisProgress = 0,
                hasNavigated = false
            )
        }
    }

    private fun loadMusicFiles() = intent {
        reduce { state.copy(isLoading = true) }

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

        reduce {
            state.copy(musicFiles = musicList, isLoading = false)
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
