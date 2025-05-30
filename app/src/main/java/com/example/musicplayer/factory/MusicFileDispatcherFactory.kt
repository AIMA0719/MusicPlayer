package com.example.musicplayer.factory

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.manager.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream

object MusicFileDispatcherFactory {

    suspend fun analyzePitchFromMediaUri(
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
                            pitchList.add(if (result.pitch > 0) result.pitch else 0f)
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

