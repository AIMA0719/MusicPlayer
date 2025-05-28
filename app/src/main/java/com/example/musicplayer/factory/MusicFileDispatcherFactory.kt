package com.example.musicplayer.factory

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.Manager.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream

object MusicFileDispatcherFactory {

    suspend fun analyzePitchFromInputStream(
        inputStream: InputStream,
        durationInMillis: Long,
        sampleRate: Int = 44100,
        bufferSize: Int = 2048,
        bufferOverlap: Int = 1024,
        onProgress: (Int) -> Unit
    ): List<Float> = withContext(Dispatchers.IO) {
        val pitchList = mutableListOf<Float>()

        val audioFormat = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val stream = BufferedInputStream(inputStream)

        val tarsosStream = object : TarsosDSPAudioInputStream {
            override fun read(b: ByteArray?, off: Int, len: Int): Int {
                val bytesRead = stream.read(b, off, len)
                LogManager.d("📦 read() returned: $bytesRead")
                return bytesRead
            }

            override fun skip(bytesToSkip: Long) { stream.skip(bytesToSkip) }
            override fun close() = stream.close()
            override fun getFormat(): TarsosDSPAudioFormat = audioFormat
            override fun getFrameLength(): Long = -1
        }

        val dispatcher = AudioDispatcher(tarsosStream, bufferSize, bufferOverlap)

        val totalFrames = (durationInMillis * sampleRate) / 1000
        val totalSteps = totalFrames / bufferSize

        var currentStep = 0
        onProgress(0) // 분석 시작

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize
        ) { result, _ ->
            val pitch = result.pitch
            if (pitch > 0) pitchList.add(pitch)

            currentStep++
            if (currentStep % 5 == 0 || currentStep == totalSteps.toInt()) {
                val progress = (currentStep.toFloat() / totalSteps * 100).toInt()
                LogManager.d("🧮 Frame $currentStep / $totalSteps (progress: $progress%)")
                onProgress(progress.coerceIn(0, 99)) // run() 이후에 100 호출
            }
        }

        dispatcher.addAudioProcessor(pitchProcessor)

        dispatcher.run()

        onProgress(100) // 분석 완료
        pitchList
    }

    suspend fun analyzePitchFromWavInputStream(
        inputStream: InputStream,
        fileLengthBytes: Long,
        sampleRate: Int = 44100,
        bufferSize: Int = 2048,
        bufferOverlap: Int = 1024,
        onProgress: (Int) -> Unit
    ): List<Float> = withContext(Dispatchers.IO) {
        val pitchList = mutableListOf<Float>()

        val audioFormat = TarsosDSPAudioFormat(
            sampleRate.toFloat(), 16, 1, true, false
        )

        val stream = BufferedInputStream(inputStream)
        var totalBytesRead = 0L

        val tarsosStream = object : TarsosDSPAudioInputStream {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val bytesRead = stream.read(b, off, len)
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead
                    val progress = ((totalBytesRead.toDouble() / fileLengthBytes) * 100).toInt()
                    onProgress(progress.coerceAtMost(99)) // 마지막 100은 run() 후 따로
                }
                return bytesRead
            }

            override fun skip(bytesToSkip: Long) {
                stream.skip(bytesToSkip)
            }

            override fun close() {
                stream.close()
            }

            override fun getFormat(): TarsosDSPAudioFormat = audioFormat
            override fun getFrameLength(): Long = -1
        }

        val dispatcher = AudioDispatcher(tarsosStream, bufferSize, bufferOverlap)

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize
        ) { result, _ ->
            if (result.pitch > 0) {
                pitchList.add(result.pitch)
            }
        }

        dispatcher.addAudioProcessor(pitchProcessor)

        dispatcher.run()

        onProgress(100) // 완료
        pitchList
    }


}

