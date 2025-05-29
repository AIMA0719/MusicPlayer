package com.example.musicplayer.scoreAlgorythm

import android.content.Context
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/**
 * WAV 파일이란? : WAV (Waveform Audio File Format)**은 디지털 오디오 데이터를 저장하는 파일 형식.
   일반적으로 PCM (Pulse Code Modulation) 형식으로 저장되며, 무손실 오디오 형식이라 압축되지 않은 고품질의 음성을 제공
 * Pitch 데이터란? : 소리의 높낮이를 의미, 음악에서 피치는 우리가 느끼는 "음의 고저"를 나타내며, 진동수(Hz)로 측정
 * 에너지란? : 에너지는 신호의 세기(Amplitude) 또는 **음량(Loudness)**을 나타냄. 이는 물리적으로 소리의 크기, 즉 소리가 얼마나 강하게 들리는지 의미함
 * -------------------------------------------------------------------------------------------------
 * 점수 (피치 데이터의 유사도) 를 뽑아내는 방법
 * 1. FFmpeg을 사용하여 입력 오디오 파일(mp4,m4a) 을 44100Hz, 모노 WAV 파일로 변환.
 * 2. TarsosDSP 라이브러리의 PitchProcessor를 사용하여 피치 데이터(Hz)를 추출. 피치 추출은 YIN 알고리즘을 사용하며, 추출된 피치를 리스트에 저장.
 * 3. 두 오디오 파일에서 추출한 피치 데이터를 비교. ( 허용 오차(50Hz) 이내의 피치만 유사도로 간주함 )
 * 4. 두 오디오 파일에서 추출한 에너지(음성 파일의 볼륨)를 비교함
 * 5. 각각 3번과 4번에서 비교하여 나온 유사도 점수를 2로 나눠서 100 곱해 총 점수를 구함 ( max 값 100 )
 * */

/**
 * Pitch 추출 알고리즘 종류와 특징
 *
 * 1. **YIN**
 *    - Autocorrelation(자기상관) 기반의 알고리즘.
 *    - 정확도가 높고, 특히 낮은 주파수 대역에서 좋은 성능을 보임.
 *    - 계산량이 많아서 처리 속도는 상대적으로 느림.
 *    - 음악 및 음성 처리에 자주 사용됨.
 *
 * 2. **FFT_YIN**
 *    - YIN 알고리즘과 FFT(Fast Fourier Transform)를 결합한 방식.
 *    - YIN보다 빠른 속도를 제공하며, 유사한 정확도를 보장.
 *    - 실시간 애플리케이션에서 사용할 수 있도록 최적화된 알고리즘.
 *
 * 3. **AMDF (Average Magnitude Difference Function)**
 *    - 평균 크기 차이 함수 기반의 알고리즘.
 *    - 단순한 구조로 인해 계산량이 적고 빠르게 동작.
 *    - 정확도는 YIN보다 떨어지지만, 간단한 애플리케이션에 적합.
 *
 * 4. **DYNAMIC_WAVELET**
 *    - Wavelet 변환 기반의 알고리즘.
 *    - 주파수와 시간 해상도가 모두 중요한 상황에서 사용 가능.
 *    - 음악 신호 분석, 다중 해상도 분석에 유리.
 *
 * 5. **MACLEOD**
 *    - FFT 기반의 빠른 주기 추정 알고리즘.
 *    - 매우 빠른 속도와 높은 정확도를 제공.
 *    - 실시간 피치 추출에 적합하며, 주로 악기 튜닝에 사용.
 *
 * --요약--
 * YIN은 높은 정확도를 자랑하지만 처리 속도가 느리고, FFT_YIN은 이를 보완하여 실시간 사용이 가능하도록 개선한 알고리즘
 * AMDF는 가장 단순한 형태로 빠르지만 정확도가 낮고, DYNAMIC_WAVELET은 복잡한 신호 분석에 적합
 * MACLEOD는 빠른 FFT 기반 알고리즘으로 속도와 정확도를 모두 갖춘 알고리즘
 */

class ScoreCalculator(
    private val context: Context,
    private var tolerance: Int = 50, // 허용 오차 (Hz), 기본값 50Hz
    private var algorithm: PitchEstimationAlgorithm = PitchEstimationAlgorithm.YIN // 피치 추출 알고리즘, 기본값 YIN
) {
    /**
     * 두 개의 오디오 파일을 비교하여 유사도를 기반으로 점수를 계산하는 함수
     * @param referenceUri 비교 기준이 되는 참조 오디오 파일의 Uri
     * @param recordedFilePath 사용자 녹음 파일의 경로
     * @return 유사도 기반의 점수 (0 ~ 100)
     */
    suspend fun compareAudioFiles(recordedFilePath: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 참조 오디오와 녹음 파일을 WAV 형식으로 변환
                val referenceWavPath = convertToWav()
                val recordedWavPath = convertToWav()

                // 2. 변환된 WAV 파일에서 피치 데이터 추출
                val referencePitches = extractPitchData(referenceWavPath)
                val recordedPitches = extractPitchData(recordedWavPath)

                // 피치 점수
                val pitchScore = calculatePitchAccuracy(referencePitches, recordedPitches)
                // 에너지 점수
                val energyScore = calculateEnergyAccuracy(referenceWavPath, recordedWavPath)
                // 스펙트럼 센트로이드 점수
                val centroidScore = calculateSpectralCentroidAccuracy(recordedFilePath,recordedWavPath)
                // 스펙트럼 롤오프 점수
                val rolloffScore = calculateSpectralRolloffAccuracy(recordedFilePath,recordedWavPath)

                val finalScore = ((pitchScore + energyScore) / 2 * 100)
                //val finalScore = (pitchScore * 0.4 + energyScore * 0.2 + centroidScore * 0.2 + rolloffScore * 0.2) * 100

                finalScore.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                0 // 오류 발생 시 0점 반환
            }
        }
    }

    /**
     * MP4/M4A 파일을 WAV 파일로 변환하는 함수
     *
     * @param uri 변환할 오디오 파일의 Uri
     * @return 변환된 WAV 파일의 경로
     */
    private fun convertToWav(): String {
        val wavPath = "${context.cacheDir}/converted_${System.currentTimeMillis()}.wav"
        return wavPath
    }

    /**
     * WAV 파일에서 피치 데이터를 추출하는 함수
     *
     * @param wavPath 피치 데이터를 추출할 WAV 파일 경로
     * @return 추출된 피치 데이터 리스트
     */
    private fun extractPitchData(wavPath: String): List<Float> {
        val pitchList = mutableListOf<Float>()

        // WAV 파일을 입력 스트림으로 읽어 AudioDispatcher 생성
        val dispatcher: AudioDispatcher = fromPipe(wavPath, 44100, 2048, 1024)

        // 피치 감지 핸들러 설정
        val pitchHandler = PitchDetectionHandler { result, _ ->
            if (result.pitch != -1f) { // 유효한 피치만 리스트에 추가
                pitchList.add(result.pitch)
            }
        }

        // 피치 처리기 추가 (YIN 알고리즘 사용)
        val pitchProcessor = PitchProcessor(algorithm, 44100f, 2048, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        // 별도 스레드에서 디스패처 실행
        val thread = Thread { dispatcher.run() }
        thread.start()
        thread.join() // 디스패처가 종료될 때까지 대기

        return pitchList
    }

    /**
     * 두 개의 피치 데이터를 비교하여 유사도를 계산하는 함수
     *
     * @param referencePitches 참조 오디오 파일에서 추출된 피치 데이터
     * @param recordedPitches 녹음 파일에서 추출된 피치 데이터
     * @return 두 피치 데이터의 유사도 (0.0 ~ 1.0)
     */
    private fun calculatePitchAccuracy(
        referencePitches: List<Float>,
        recordedPitches: List<Float>
    ): Double {
        val minSize = minOf(referencePitches.size, recordedPitches.size) // 두 리스트 중 작은 크기로 비교
        var correctCount = 0

        for (i in 0 until minSize) {
            val refPitch = referencePitches[i]
            val recordedPitch = recordedPitches[i]

            // 허용 오차 이내의 피치만 유사도로 간주
            if (abs(refPitch - recordedPitch) <= tolerance) {
                correctCount++
            }
        }

        return if (minSize > 0) correctCount.toDouble() / minSize else 0.0
    }

    /**
     * 두 WAV 파일의 에너지 유사도를 계산하는 함수.
     * RMS(Root Mean Square) 에너지를 사용하여 두 파일의 에너지 차이를 비교하고,
     * 차이가 클수록 낮은 점수를 반환하도록 구현.
     *
     * @param referenceWavPath 원본 오디오 파일의 WAV 경로
     * @param recordedWavPath 녹음된 오디오 파일의 WAV 경로
     * @return 에너지 유사도 점수 (0.0 ~ 1.0 사이의 값)
     */
    private fun calculateEnergyAccuracy(referenceWavPath: String, recordedWavPath: String): Double {
        // 두 WAV 파일의 RMS 에너지 계산
        val refEnergy = calculateRMS(referenceWavPath)
        val recEnergy = calculateRMS(recordedWavPath)

        // 두 에너지 값의 차이를 계산
        val energyDifference = abs(refEnergy - recEnergy)
        // 두 에너지 값 중 더 큰 값을 계산
        val maxEnergy = maxOf(refEnergy, recEnergy)

        // 에너지 차이가 클수록 낮은 점수를 반환
        // 1에서 에너지 차이 비율을 뺀 값을 반환하여 유사도 점수를 계산
        return if (maxEnergy > 0) 1.0 - (energyDifference / maxEnergy) else 0.0
    }

    /**
     * WAV 파일의 RMS(Root Mean Square) 에너지를 계산하는 함수.
     * WAV 파일을 바이트 단위로 읽어서 RMS 에너지를 계산하며,
     * 에너지는 음성 신호의 세기를 나타내는 데 사용됨.
     *
     * @param wavPath WAV 파일의 경로
     * @return RMS 에너지 값
     */
    private fun calculateRMS(wavPath: String): Double {
        val audioFile = File(wavPath)
        val inputStream = FileInputStream(audioFile)

        val buffer = ByteArray(2048) // 버퍼 크기 설정
        var sum = 0.0               // 제곱합을 저장할 변수
        var count = 0               // 읽은 샘플 수를 저장할 변수

        // 파일의 모든 데이터를 읽을 때까지 반복
        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break // 더 이상 읽을 데이터가 없으면 종료

            // 읽은 바이트 배열에서 각 샘플 값을 제곱하여 합산
            for (i in 0 until bytesRead) {
                sum += buffer[i] * buffer[i]
            }
            count += bytesRead // 전체 샘플 수 누적
        }

        inputStream.close() // 파일 스트림 닫기

        // RMS 에너지 계산: 제곱합을 샘플 수로 나눈 후 제곱근을 취함
        return if (count > 0) Math.sqrt(sum / count) else 0.0
    }

    /**
     * 스펙트럼 센트로이드 기반 점수 계산
     * 스펙트럼 센트로이드는 소리의 **밝기(Brightness)**를 측정하는 지표입니다.
     * 주파수 성분이 높을수록 밝은 소리로 인식되며, 두 파일 간 스펙트럼 센트로이드 값의 차이가 적을수록 유사한 소리로 판단할 수 있습니다.
     * */

    private fun calculateSpectralCentroidAccuracy(referenceWavPath: String, recordedWavPath: String): Double {
        val refCentroid = calculateSpectralCentroid(referenceWavPath)
        val recCentroid = calculateSpectralCentroid(recordedWavPath)

        val centroidDifference = abs(refCentroid - recCentroid)
        val maxCentroid = maxOf(refCentroid, recCentroid)

        // 두 값의 차이를 기준으로 유사도를 계산 (차이가 작을수록 높은 점수)
        return if (maxCentroid > 0) 1.0 - (centroidDifference / maxCentroid) else 0.0
    }

    private fun calculateSpectralCentroid(wavPath: String): Double {
        val audioFile = File(wavPath)
        val inputStream = FileInputStream(audioFile)
        val buffer = ByteArray(2048)
        var weightedSum = 0.0
        var totalAmplitude = 0.0
        var count = 0

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break

            for (i in 0 until bytesRead) {
                val frequency = i.toDouble() // 주파수 인덱스
                val amplitude = buffer[i].toDouble() // 해당 주파수의 진폭
                weightedSum += frequency * amplitude
                totalAmplitude += amplitude
            }
            count += bytesRead
        }
        inputStream.close()

        return if (totalAmplitude > 0) weightedSum / totalAmplitude else 0.0
    }


    /**
     * 스펙트럼 롤오프 기반 점수 계산
     * 스펙트럼 롤오프는 주파수 스펙트럼에서 **에너지의 일정 비율(예: 85%)**이 포함되는 주파수 경계 지점을 의미합니다.
     * 두 파일 간 스펙트럼 롤오프 값을 비교하여 소리의 유사도를 판단할 수 있습니다.
     * */
    private fun calculateSpectralRolloffAccuracy(referenceWavPath: String, recordedWavPath: String): Double {
        val refRolloff = calculateSpectralRolloff(referenceWavPath)
        val recRolloff = calculateSpectralRolloff(recordedWavPath)

        val rolloffDifference = abs(refRolloff - recRolloff)
        val maxRolloff = maxOf(refRolloff, recRolloff)

        return if (maxRolloff > 0) 1.0 - (rolloffDifference / maxRolloff) else 0.0
    }

    private fun calculateSpectralRolloff(wavPath: String): Double {
        val audioFile = File(wavPath)
        val inputStream = FileInputStream(audioFile)
        val buffer = ByteArray(2048)
        var cumulativeEnergy = 0.0
        var totalEnergy = 0.0
        var rolloffFrequency = 0.0

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break

            for (i in 0 until bytesRead) {
                val amplitude = buffer[i].toDouble()
                totalEnergy += amplitude
            }
        }

        inputStream.close()
        val threshold = totalEnergy * 0.85
        cumulativeEnergy = 0.0

        for (i in buffer.indices) {
            cumulativeEnergy += buffer[i].toDouble()
            if (cumulativeEnergy >= threshold) {
                rolloffFrequency = i.toDouble()
                break
            }
        }

        return rolloffFrequency
    }



    /**
     * WAV 파일을 입력으로 받아 AudioDispatcher를 생성하는 함수
     *
     * @param wavPath 입력 WAV 파일 경로
     * @param sampleRate 샘플레이트 (Hz)
     * @param bufferSize 버퍼 크기
     * @param overlap 오버랩 크기
     * @return AudioDispatcher 객체
     */
    private fun fromPipe(wavPath: String, sampleRate: Int, bufferSize: Int, overlap: Int): AudioDispatcher {
        val audioFile = File(wavPath)
        val inputStream = FileInputStream(audioFile) // 파일 입력 스트림 생성

        val audioFormat = TarsosDSPAudioFormat(
            sampleRate.toFloat(), // 샘플레이트
            16,                   // 비트 깊이 (16비트)
            1,                    // 채널 수 (모노)
            true,                 // signed
            false                 // bigEndian
        )

        // UniversalAudioInputStream을 사용하여 입력 스트림과 포맷을 감싸서 AudioDispatcher 생성
        val universalAudioInputStream = UniversalAudioInputStream(inputStream, audioFormat)
        return AudioDispatcher(universalAudioInputStream, bufferSize, overlap)
    }
}
