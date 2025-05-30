package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.MusicListState
import com.example.musicplayer.manager.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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

    private suspend fun analyzePitchFromMediaUri(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit
    ): List<Float> = withContext(Dispatchers.IO) {

        val pitchList = mutableListOf<Float>()
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(context, uri, null)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext emptyList()

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            val bufferInfo = MediaCodec.BufferInfo()

            var sawInputEOS = false
            var sawOutputEOS = false

            val bufferSize = 2048
            val bufferOverlap = 1024
            var totalDecodedBytes = 0L

            val durationUs = inputFormat.getLong(MediaFormat.KEY_DURATION)

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = inputBuffers[inputBufferIndex]
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = outputBuffers[outputBufferIndex]
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputBuffer.clear()
                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    totalDecodedBytes += chunk.size

                    // PCM 데이터를 바로 분석
                    val inputStream = ByteArrayInputStream(chunk)
                    val audioFormat = TarsosDSPAudioFormat(
                        sampleRate.toFloat(), 16, 1, true, false
                    )
                    val tarsosStream = UniversalAudioInputStream(inputStream, audioFormat)
                    val dispatcher = AudioDispatcher(tarsosStream, bufferSize, bufferOverlap)

                    dispatcher.addAudioProcessor(
                        PitchProcessor(
                            PitchProcessor.PitchEstimationAlgorithm.YIN,
                            sampleRate.toFloat(),
                            bufferSize
                        ) { result, _ ->
                            if (result.pitch > 0) pitchList.add(result.pitch)
                        }
                    )

                    dispatcher.run()

                    // 진행률 업데이트 (추정 기반)
                    val progress = ((bufferInfo.presentationTimeUs / durationUs.toDouble()) * 100).toInt()
                    onProgress(progress.coerceIn(0, 99))

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            onProgress(100)
            pitchList

        } catch (e: Exception) {
            e.printStackTrace()
            extractor.release()
            onProgress(100)
            emptyList()
        }
    }

}
