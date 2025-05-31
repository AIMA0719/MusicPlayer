package com.example.musicplayer.scoreAlgorythm

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 점수 계산을 위한 알고리즘 클래스
 * - pitchListOriginal: 원곡의 피치 값 리스트
 * - pitchListUser: 사용자의 피치 값 리스트
 * - toleranceHz: 피치 비교 시 허용 오차 (Hz)
 */
class ScoreAnalyzer(
    private val pitchListOriginal: List<Float>,
    private val pitchListUser: List<Float>,
    private val toleranceHz: Float = 50f // 허용 오차 Hz
) {

    /**
     * 전체 점수 계산
     * - 피치, 에너지, 스펙트럼 중심 주파수, 롤오프 평균을 통해 최종 점수를 계산
     */
    fun calculateTotalScore(): Int {
        val pitchScore = calculatePitchSimilarity()
        val energyScore = calculateEnergySimilarity()
        val centroidScore = calculateSpectralCentroidSimilarity()
        val rolloffScore = calculateSpectralRolloffSimilarity()

        val average = (pitchScore + energyScore + centroidScore + rolloffScore) / 4.0
        return (average * 100).toInt()
    }

    /**
     * 피치 유사도 계산 (허용 오차 이내의 비율)
     */
    private fun calculatePitchSimilarity(): Double {
        val minSize = minOf(pitchListOriginal.size, pitchListUser.size)
        if (minSize == 0) return 0.0

        var matchedCount = 0
        for (i in 0 until minSize) {
            val original = pitchListOriginal[i]
            val user = pitchListUser[i]
            if (abs(original - user) <= toleranceHz) {
                matchedCount++
            }
        }
        return matchedCount.toDouble() / minSize
    }

    /**
     * RMS 에너지 기반 유사도 계산
     */
    private fun calculateEnergySimilarity(): Double {
        val rmsOriginal = calculateRMS(pitchListOriginal)
        val rmsUser = calculateRMS(pitchListUser)

        val diff = abs(rmsOriginal - rmsUser)
        val maxEnergy = maxOf(rmsOriginal, rmsUser)
        return if (maxEnergy > 0) 1.0 - (diff / maxEnergy) else 0.0
    }

    /**
     * Spectral Centroid 기반 유사도 계산
     * - 가중 평균 피치값을 사용해 중심 주파수의 차이를 비교
     */
    private fun calculateSpectralCentroidSimilarity(): Double {
        val centroidOriginal = calculateSpectralCentroid(pitchListOriginal)
        val centroidUser = calculateSpectralCentroid(pitchListUser)

        val diff = abs(centroidOriginal - centroidUser)
        val max = maxOf(centroidOriginal, centroidUser)
        return if (max > 0) 1.0 - (diff / max) else 0.0
    }

    /**
     * Spectral Rolloff 기반 유사도 계산
     * - 상위 85% 에너지에 도달하는 인덱스를 기준으로 계산
     */
    private fun calculateSpectralRolloffSimilarity(): Double {
        val rolloffOriginal = calculateSpectralRolloff(pitchListOriginal)
        val rolloffUser = calculateSpectralRolloff(pitchListUser)

        val diff = abs(rolloffOriginal - rolloffUser)
        val max = maxOf(rolloffOriginal, rolloffUser)
        return if (max > 0) 1.0 - (diff / max) else 0.0
    }

    // --- 내부 계산 메서드 ---
    private fun calculateRMS(data: List<Float>): Double {
        if (data.isEmpty()) return 0.0
        val sumSquares = data.sumOf { (it * it).toDouble() }
        return sqrt(sumSquares / data.size)
    }

    private fun calculateSpectralCentroid(data: List<Float>): Double {
        if (data.isEmpty()) return 0.0
        val weightedSum = data.indices.sumOf { i -> (i.toDouble() * data[i]) }
        val total = data.sumOf { it.toDouble() }
        return if (total > 0.0) weightedSum / total else 0.0
    }

    private fun calculateSpectralRolloff(data: List<Float>, threshold: Double = 0.85): Int {
        if (data.isEmpty()) return 0
        val totalEnergy = data.sumOf { it.toDouble() }
        val thresholdEnergy = totalEnergy * threshold
        var cumulativeEnergy = 0.0

        for ((i, value) in data.withIndex()) {
            cumulativeEnergy += value
            if (cumulativeEnergy >= thresholdEnergy) {
                return i
            }
        }
        return data.size - 1
    }
}
