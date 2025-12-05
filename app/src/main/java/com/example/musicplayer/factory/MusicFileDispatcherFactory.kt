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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

object MusicFileDispatcherFactory {

    // 버퍼 크기 상수 (최적화된 값)
    private const val CODEC_TIMEOUT_US = 5000L  // 5ms (더 짧은 타임아웃으로 응답성 향상)
    private const val OUTPUT_BUFFER_SIZE = 64 * 1024  // 64KB 출력 버퍼

    suspend fun analyzePitchFromMediaUri(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit
    ): List<Float> = withContext(Dispatchers.Default) {
        val pitchList = mutableListOf<Float>()
        val extractor = MediaExtractor()

        // 메모리 기반 PCM 스트림 (파일 IO 제거)
        val pcmOutputStream = ByteArrayOutputStream(OUTPUT_BUFFER_SIZE)

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

            // 예상 PCM 바이트 수 (모노로 변환될 크기)
            val pcmBytesPerSecond = sampleRate * 2  // 16bit mono = 2 bytes per sample
            val totalExpectedPcmBytes = (durationUs / 1_000_000.0 * pcmBytesPerSecond).toLong()

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()

            var sawInputEOS = false
            var sawOutputEOS = false
            var writtenBytes: Long = 0
            var lastProgressUpdate = 0

            // 디코딩 루프 (최적화)
            while (!sawOutputEOS && coroutineContext.isActive) {
                // 입력 버퍼 처리
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.let { buffer ->
                            val sampleSize = extractor.readSampleData(buffer, 0)

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
                }

                // 출력 버퍼 처리 (연속으로 여러 개 처리)
                var outputProcessed = true
                while (outputProcessed && !sawOutputEOS) {
                    val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        outputBuffer?.let { buffer ->
                            // 스테레오를 모노로 변환하며 직접 메모리에 쓰기
                            val monoData = convertToMono(buffer, bufferInfo.size, channelCount)
                            pcmOutputStream.write(monoData)
                            writtenBytes += monoData.size

                            buffer.clear()
                            codec.releaseOutputBuffer(outputBufferIndex, false)

                            // 진행률 업데이트 (1% 단위로 업데이트)
                            val decodeProgress = ((writtenBytes.toDouble() / totalExpectedPcmBytes) * 75).toInt()
                                .coerceIn(0, 75)
                            if (decodeProgress >= lastProgressUpdate + 1) {
                                lastProgressUpdate = decodeProgress
                                onProgress(decodeProgress)
                            }
                        }

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEOS = true
                        }
                    } else {
                        outputProcessed = false
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            onProgress(75)

            // 취소 확인
            if (!coroutineContext.isActive) {
                return@withContext emptyList()
            }

            // 분석 단계 시작 (메모리 기반)
            val pcmData = pcmOutputStream.toByteArray()
            val audioFormat = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val tarsosStream = UniversalAudioInputStream(ByteArrayInputStream(pcmData), audioFormat)

            // 버퍼 크기 최적화: 50ms 단위 (더 빠른 처리)
            val bufferSize = sampleRate / 20  // 50ms 단위
            val bufferOverlap = 0
            val dispatcher = AudioDispatcher(tarsosStream, bufferSize, bufferOverlap)

            val totalSamples = pcmData.size / 2  // 16bit = 2 bytes
            var processedSamples = 0

            dispatcher.addAudioProcessor(
                PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.YIN,
                    sampleRate.toFloat(),
                    bufferSize
                ) { result, _ ->
                    pitchList.add(if (result.pitch > 0) result.pitch else 0f)
                    processedSamples += bufferSize
                    val analyzeProgress = 75 + ((processedSamples.toDouble() / totalSamples) * 24).toInt()
                    if (analyzeProgress > lastProgressUpdate) {
                        lastProgressUpdate = analyzeProgress
                        onProgress(analyzeProgress.coerceIn(76, 99))
                    }
                }
            )

            dispatcher.run()
            onProgress(100)

            // 50ms 단위를 100ms 단위로 다운샘플링 (2개씩 평균)
            downsamplePitchList(pitchList)

        } catch (e: Exception) {
            e.printStackTrace()
            try { extractor.release() } catch (_: Exception) {}
            onProgress(100)
            emptyList()
        }
    }

    /**
     * 스테레오 PCM을 모노로 변환 (최적화)
     */
    private fun convertToMono(buffer: ByteBuffer, size: Int, channelCount: Int): ByteArray {
        if (channelCount == 1) {
            // 이미 모노
            val result = ByteArray(size)
            buffer.get(result)
            return result
        }

        // 스테레오 → 모노 변환 (L+R / 2)
        val monoSize = size / channelCount
        val result = ByteArray(monoSize)
        val shortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samplesPerChannel = size / (2 * channelCount)

        var outIdx = 0
        for (i in 0 until samplesPerChannel) {
            var sum = 0
            for (ch in 0 until channelCount) {
                sum += shortBuffer.get(i * channelCount + ch).toInt()
            }
            val monoSample = (sum / channelCount).toShort()

            // Little-endian으로 쓰기
            result[outIdx++] = (monoSample.toInt() and 0xFF).toByte()
            result[outIdx++] = ((monoSample.toInt() shr 8) and 0xFF).toByte()
        }

        return result
    }

    /**
     * 피치 리스트 다운샘플링 (50ms → 100ms)
     * 인접한 2개 샘플의 평균 (무음 제외)
     */
    private fun downsamplePitchList(pitchList: MutableList<Float>): List<Float> {
        if (pitchList.size <= 1) return pitchList

        val result = mutableListOf<Float>()
        var i = 0
        while (i < pitchList.size) {
            if (i + 1 < pitchList.size) {
                val p1 = pitchList[i]
                val p2 = pitchList[i + 1]

                // 둘 다 무음이면 무음, 하나라도 소리가 있으면 평균
                val averaged = when {
                    p1 <= 0 && p2 <= 0 -> 0f
                    p1 <= 0 -> p2
                    p2 <= 0 -> p1
                    else -> (p1 + p2) / 2
                }
                result.add(averaged)
                i += 2
            } else {
                result.add(pitchList[i])
                i++
            }
        }

        pitchList.clear()
        pitchList.addAll(result)
        return result
    }
}
