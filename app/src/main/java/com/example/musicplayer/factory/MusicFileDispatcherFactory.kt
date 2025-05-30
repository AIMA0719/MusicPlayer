package com.example.musicplayer.factory

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object MusicFileDispatcherFactory {

    suspend fun analyzePitchFromMediaUri(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit
    ): List<Float> = withContext(Dispatchers.IO) {
        val pitchList = mutableListOf<Float>()
        val extractor = MediaExtractor()

        // 임시 PCM 파일 생성
        val tempPcmFile = File.createTempFile("decoded_pcm_", ".pcm", context.cacheDir)
        val pcmOutputStream = BufferedOutputStream(FileOutputStream(tempPcmFile))

        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext emptyList()

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val durationUs = inputFormat.getLong(MediaFormat.KEY_DURATION)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val pcmBytesPerSecond = sampleRate * channelCount * 2 // 16bit = 2 bytes
            val totalExpectedPcmBytes = (durationUs / 1_000_000.0 * pcmBytesPerSecond).toLong()

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            val bufferInfo = MediaCodec.BufferInfo()

            var sawInputEOS = false
            var sawOutputEOS = false
            var writtenBytes: Long = 0

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = inputBuffers[inputBufferIndex]
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0
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

                    pcmOutputStream.write(chunk)
                    writtenBytes += chunk.size

                    val decodeProgress = ((writtenBytes.toDouble() / totalExpectedPcmBytes) * 80).toInt()
                    onProgress(decodeProgress.coerceIn(0, 80)) // 디코딩 단계는 0~80%
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            pcmOutputStream.flush()
            pcmOutputStream.close()
            codec.stop()
            codec.release()
            extractor.release()

            // 분석 단계 시작
            val audioFormat = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val inputStream = BufferedInputStream(FileInputStream(tempPcmFile))
            val tarsosStream = UniversalAudioInputStream(inputStream, audioFormat)

            val bufferSize = sampleRate / 10 // 100ms 단위
            val bufferOverlap = 0
            val dispatcher = AudioDispatcher(tarsosStream, bufferSize, bufferOverlap)

            val totalBytes = tempPcmFile.length()
            var processedBytes = 0L

            dispatcher.addAudioProcessor(
                PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.YIN,
                    sampleRate.toFloat(),
                    bufferSize
                ) { result, _ ->
                    pitchList.add(if (result.pitch > 0) result.pitch else 0f)
                    processedBytes += bufferSize * 2L // 16bit = 2 bytes
                    val analyzeProgress = 80 + ((processedBytes.toDouble() / totalBytes) * 20).toInt()
                    onProgress(analyzeProgress.coerceIn(81, 99)) // 분석 단계는 81~99%
                }
            )

            dispatcher.run()
            onProgress(100)
            tempPcmFile.delete()
            pitchList

        } catch (e: Exception) {
            e.printStackTrace()
            try { pcmOutputStream.close() } catch (_: IOException) {}
            tempPcmFile.delete()
            extractor.release()
            onProgress(100)
            emptyList()
        }
    }
}
