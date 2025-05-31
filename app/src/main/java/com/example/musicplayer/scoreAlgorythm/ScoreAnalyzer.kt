package com.example.musicplayer.scoreAlgorythm

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 점수 계산을 위한 알고리즘 클래스
 * - pitchListOriginal: 원곡의 피치 값 리스트
 * - pitchListUser: 사용자의 피치 값 리스트
 * 두 리스트를 비교하여 유사도를 계산하고, RMS 에너지 차이도 함께 반영하여 점수를 계산
 */
class ScoreAnalyzer(
    private val pitchListOriginal: List<Float>,
    private val pitchListUser: List<Float>,
    private val toleranceHz: Float = 50f // 허용 오차 Hz
) {

    /**
     * 전체 점수 계산
     * - 피치 유사도 점수와 RMS 에너지 유사도 점수를 평균하여 0~100 사이의 점수 반환
     */
    fun calculateTotalScore(): Int {
        val pitchScore = calculatePitchSimilarity()
        val energyScore = calculateEnergySimilarity()

        // 평균을 내어 총 점수 반환 (0~100)
        return ((pitchScore + energyScore) / 2 * 100).toInt()
    }

    /**
     * 피치 유사도 계산
     * - 두 리스트 간의 동일 인덱스에서의 피치 차이가 허용 오차 이내인 경우를 세서 비율로 점수 환산
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
     * RMS (Root Mean Square) 에너지를 기반으로 한 유사도 계산
     * - 두 리스트의 에너지값을 계산 후, 차이를 기반으로 유사도 반환
     */
    private fun calculateEnergySimilarity(): Double {
        val rmsOriginal = calculateRMS(pitchListOriginal)
        val rmsUser = calculateRMS(pitchListUser)

        val diff = abs(rmsOriginal - rmsUser)
        val maxEnergy = maxOf(rmsOriginal, rmsUser)
        return if (maxEnergy > 0) 1.0 - (diff / maxEnergy) else 0.0
    }

    /**
     * RMS (Root Mean Square) 에너지 계산
     * - 리스트의 제곱 평균 값의 제곱근 반환
     */
    private fun calculateRMS(data: List<Float>): Double {
        if (data.isEmpty()) return 0.0

        val sumSquares = data.map { it * it }.sum().toDouble()
        return sqrt(sumSquares / data.size)
    }
}
