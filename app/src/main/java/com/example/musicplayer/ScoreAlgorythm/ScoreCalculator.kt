package com.example.musicplayer.Manager

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class ScoreCalculator(private val context: Context) {

    // 두 개의 오디오 파일을 비교하여 스코어 계산
    suspend fun compareAudioFiles(referenceUri: Uri, recordedFilePath: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                // 1. MP4/M4A 파일을 WAV로 변환
                val referenceWavPath = convertToWav(referenceUri)
                val recordedWavPath = convertToWav(File(recordedFilePath).toUri())

                // 2. 피치 데이터 추출
                val referencePitches = extractPitchData(referenceWavPath)
                val recordedPitches = extractPitchData(recordedWavPath)

                // 3. 피치 유사도 계산
                val score = calculatePitchAccuracy(referencePitches, recordedPitches)

                // 4. 스코어 반환 (0 ~ 100)
                (score * 100).toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                0 // 오류 발생 시 0점 반환
            }
        }
    }

    // MP4/M4A 파일을 WAV로 변환하는 함수
    private fun convertToWav(uri: Uri): String {
        val inputFilePath = UriUtils.copyUriToTempFile(uri, context)
        val wavPath = "${context.cacheDir}/converted_${System.currentTimeMillis()}.wav"
        val command = "-i $inputFilePath -ar 44100 -ac 1 -y $wavPath"

        val result = FFmpeg.execute(command)
        if (result != 0) {
            throw RuntimeException("Error converting audio to WAV: $result")
        }
        return wavPath
    }

    // WAV 파일에서 피치 데이터를 추출하는 함수
    private fun extractPitchData(wavPath: String): List<Float> {
        val pitchList = mutableListOf<Float>()
        val dispatcher: AudioDispatcher = fromPipe(wavPath, 44100, 2048, 1024)
        val pitchHandler = PitchDetectionHandler { result, _ ->
            if (result.pitch != -1f) {
                pitchList.add(result.pitch)
            }
        }
        val pitchProcessor = PitchProcessor(PitchEstimationAlgorithm.YIN, 44100f, 2048, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        val thread = Thread {
            dispatcher.run()
        }
        thread.start()
        thread.join() // Dispatcher가 종료될 때까지 대기

        return pitchList
    }

    // 두 개의 피치 데이터를 비교하여 유사도를 계산하는 함수
    private fun calculatePitchAccuracy(referencePitches: List<Float>, recordedPitches: List<Float>): Double {
        val minSize = minOf(referencePitches.size, recordedPitches.size)
        var correctCount = 0

        for (i in 0 until minSize) {
            val refPitch = referencePitches[i]
            val recordedPitch = recordedPitches[i]
            if (Math.abs(refPitch - recordedPitch) <= 50) { // 허용 오차 50Hz
                correctCount++
            }
        }

        return if (minSize > 0) correctCount.toDouble() / minSize else 0.0
    }

    private fun fromPipe(wavPath: String, sampleRate: Int, bufferSize: Int, overlap: Int): AudioDispatcher {
        // WAV 파일을 AudioInputStream으로 읽기
        val audioFile = File(wavPath)
        val inputStream = FileInputStream(audioFile)

        val audioFormat = TarsosDSPAudioFormat(
            sampleRate.toFloat(), // 샘플레이트
            16,                   // 비트 깊이 (16비트)
            1,                    // 채널 수 (모노)
            true,                 // signed
            false                 // bigEndian
        )

        // UniversalAudioInputStream으로 파일 스트림을 감싸서 AudioDispatcher 생성
        val universalAudioInputStream = UniversalAudioInputStream(inputStream, audioFormat)
        return AudioDispatcher(universalAudioInputStream, bufferSize, overlap)
    }
}
