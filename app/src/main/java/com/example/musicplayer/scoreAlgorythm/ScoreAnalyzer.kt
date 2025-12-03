package com.example.musicplayer.scoreAlgorythm

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 개선된 점수 계산 알고리즘
 *
 * 주요 개선사항:
 * - DTW(Dynamic Time Warping) 기반 시간 정렬
 * - 리듬/타이밍 평가 추가
 * - 볼륨 안정성 평가
 * - 무음 구간 처리
 * - 길이 차이 페널티
 * - 과학적 가중치 조정 (음정 50%, 리듬 30%, 볼륨 15%, 길이 5%)
 *
 * @param pitchListOriginal 원곡의 피치 값 리스트
 * @param pitchListUser 사용자의 피치 값 리스트
 * @param toleranceHz 피치 비교 시 허용 오차 (Hz)
 * @param silenceThreshold 무음으로 간주할 피치 임계값 (Hz)
 */
class ScoreAnalyzer(
    private val pitchListOriginal: List<Float>,
    private val pitchListUser: List<Float>,
    private val toleranceHz: Float = 50f,
    private val silenceThreshold: Float = 80f
) {

    /**
     * 전체 점수 계산 (개선된 알고리즘)
     *
     * 가중치:
     * - 음정 정확도: 50%
     * - 리듬/타이밍: 30%
     * - 볼륨 안정성: 15%
     * - 길이 매칭: 5%
     */
    fun calculateTotalScore(): Int {
        if (pitchListOriginal.isEmpty() || pitchListUser.isEmpty()) {
            return 0
        }

        val pitchScore = calculatePitchAccuracyWithDTW() * 0.50
        val rhythmScore = calculateRhythmScore() * 0.30
        val volumeScore = calculateVolumeStability() * 0.15
        val durationScore = calculateDurationMatch() * 0.05

        val totalScore = (pitchScore + rhythmScore + volumeScore + durationScore) * 100
        return totalScore.toInt().coerceIn(0, 100)
    }

    // ==================== DTW 기반 음정 정확도 ====================

    /**
     * DTW(Dynamic Time Warping) 알고리즘을 사용한 음정 정확도 계산
     * 템포 변화를 허용하여 더 정확한 평가 가능
     */
    private fun calculatePitchAccuracyWithDTW(): Double {
        val alignment = calculateDTWAlignment(pitchListOriginal, pitchListUser)

        if (alignment.isEmpty()) return 0.0

        var totalError = 0.0
        var validCount = 0

        for ((origIdx, userIdx) in alignment) {
            val origPitch = pitchListOriginal[origIdx]
            val userPitch = pitchListUser[userIdx]

            // 둘 다 무음인 경우는 정확한 것으로 간주
            if (isSilence(origPitch) && isSilence(userPitch)) {
                validCount++
                continue
            }

            // 한쪽만 무음인 경우는 오류로 간주
            if (isSilence(origPitch) != isSilence(userPitch)) {
                totalError += 1.0
                validCount++
                continue
            }

            // 둘 다 소리가 있는 경우
            val error = abs(origPitch - userPitch) / toleranceHz
            totalError += min(error.toDouble(), 1.0) // 최대 오류를 1로 제한
            validCount++
        }

        return if (validCount > 0) {
            1.0 - (totalError / validCount)
        } else {
            0.0
        }
    }

    /**
     * DTW 알고리즘으로 두 시퀀스를 정렬
     * @return 정렬된 인덱스 쌍의 리스트 (원곡 인덱스, 사용자 인덱스)
     */
    private fun calculateDTWAlignment(seq1: List<Float>, seq2: List<Float>): List<Pair<Int, Int>> {
        val n = seq1.size
        val m = seq2.size

        if (n == 0 || m == 0) return emptyList()

        // DTW 거리 행렬 초기화
        val dtw = Array(n + 1) { DoubleArray(m + 1) { Double.POSITIVE_INFINITY } }
        dtw[0][0] = 0.0

        // DTW 거리 계산
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = calculatePitchDistance(seq1[i - 1], seq2[j - 1])
                dtw[i][j] = cost + minOf(
                    dtw[i - 1][j],     // 삽입
                    dtw[i][j - 1],     // 삭제
                    dtw[i - 1][j - 1]  // 매칭
                )
            }
        }

        // 백트래킹으로 정렬 경로 추출
        val alignment = mutableListOf<Pair<Int, Int>>()
        var i = n
        var j = m

        while (i > 0 && j > 0) {
            alignment.add(0, Pair(i - 1, j - 1))

            val diagonal = dtw[i - 1][j - 1]
            val left = dtw[i][j - 1]
            val up = dtw[i - 1][j]

            when (minOf(diagonal, left, up)) {
                diagonal -> {
                    i--
                    j--
                }
                up -> i--
                else -> j--
            }
        }

        return alignment
    }

    /**
     * 두 피치 값 간의 거리 계산
     */
    private fun calculatePitchDistance(pitch1: Float, pitch2: Float): Double {
        val isSilence1 = isSilence(pitch1)
        val isSilence2 = isSilence(pitch2)

        return when {
            isSilence1 && isSilence2 -> 0.0 // 둘 다 무음
            isSilence1 != isSilence2 -> 1.0 // 한쪽만 무음
            else -> (abs(pitch1 - pitch2) / toleranceHz).toDouble().coerceIn(0.0, 1.0)
        }
    }

    // ==================== 리듬/타이밍 평가 ====================

    /**
     * 리듬 및 타이밍 정확도 평가
     * Voice Activity Detection을 통해 노트 시작/종료 시점 비교
     */
    private fun calculateRhythmScore(): Double {
        val originalOnsets = detectOnsets(pitchListOriginal)
        val userOnsets = detectOnsets(pitchListUser)

        if (originalOnsets.isEmpty() || userOnsets.isEmpty()) {
            return 0.5 // 기본 점수
        }

        // DTW로 온셋 시퀀스 정렬
        val onsetAlignment = alignOnsets(originalOnsets, userOnsets)

        if (onsetAlignment.isEmpty()) return 0.5

        var totalTimingError = 0.0

        for ((origIdx, userIdx) in onsetAlignment) {
            val origOnset = originalOnsets[origIdx]
            val userOnset = userOnsets[userIdx]

            // 타이밍 오차 계산 (100ms 단위로 정규화)
            val timingError = abs(origOnset - userOnset) / 10.0 // 10 인덱스 = 1초
            totalTimingError += min(timingError, 1.0)
        }

        val avgError = totalTimingError / onsetAlignment.size
        return 1.0 - avgError
    }

    /**
     * 음성 시작 지점(Onset) 검출
     * @return 온셋 인덱스 리스트
     */
    private fun detectOnsets(pitchList: List<Float>): List<Int> {
        val onsets = mutableListOf<Int>()
        var wasVoice = false

        for (i in pitchList.indices) {
            val isVoice = !isSilence(pitchList[i])

            // 무음에서 소리로 전환되는 지점
            if (isVoice && !wasVoice) {
                onsets.add(i)
            }

            wasVoice = isVoice
        }

        return onsets
    }

    /**
     * 두 온셋 시퀀스를 정렬
     */
    private fun alignOnsets(onsets1: List<Int>, onsets2: List<Int>): List<Pair<Int, Int>> {
        val n = onsets1.size
        val m = onsets2.size

        if (n == 0 || m == 0) return emptyList()

        // 간단한 DTW
        val dtw = Array(n + 1) { DoubleArray(m + 1) { Double.POSITIVE_INFINITY } }
        dtw[0][0] = 0.0

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = abs(onsets1[i - 1] - onsets2[j - 1]).toDouble()
                dtw[i][j] = cost + minOf(dtw[i - 1][j], dtw[i][j - 1], dtw[i - 1][j - 1])
            }
        }

        // 백트래킹
        val alignment = mutableListOf<Pair<Int, Int>>()
        var i = n
        var j = m

        while (i > 0 && j > 0) {
            alignment.add(0, Pair(i - 1, j - 1))

            val diagonal = dtw[i - 1][j - 1]
            val left = dtw[i][j - 1]
            val up = dtw[i - 1][j]

            when (minOf(diagonal, left, up)) {
                diagonal -> { i--; j-- }
                up -> i--
                else -> j--
            }
        }

        return alignment
    }

    // ==================== 볼륨 안정성 평가 ====================

    /**
     * 볼륨 안정성 평가
     * 음성이 있는 구간에서 일정한 볼륨을 유지하는지 평가
     */
    private fun calculateVolumeStability(): Double {
        // 무음이 아닌 구간의 피치만 추출
        val voicePitches = pitchListUser.filter { !isSilence(it) }

        if (voicePitches.size < 2) {
            return 0.5 // 기본 점수
        }

        // 표준편차 계산 (변동성 측정)
        val mean = voicePitches.average()
        val variance = voicePitches.sumOf { (it - mean) * (it - mean) } / voicePitches.size
        val stdDev = sqrt(variance)

        // 변동 계수 (Coefficient of Variation)
        val cv = if (mean > 0) stdDev / mean else 1.0

        // CV가 낮을수록 안정적 (0.3 이하면 매우 안정적)
        return when {
            cv < 0.2 -> 1.0
            cv < 0.3 -> 0.9
            cv < 0.5 -> 0.7
            cv < 0.7 -> 0.5
            else -> 0.3
        }
    }

    // ==================== 길이 매칭 평가 ====================

    /**
     * 노래 길이 매칭 평가
     * 너무 짧게 또는 길게 부른 경우 페널티
     */
    private fun calculateDurationMatch(): Double {
        val originalLength = pitchListOriginal.size
        val userLength = pitchListUser.size

        if (originalLength == 0) return 0.0

        val ratio = userLength.toDouble() / originalLength

        return when {
            ratio in 0.9..1.1 -> 1.0      // ±10% 이내: 완벽
            ratio in 0.8..1.2 -> 0.8      // ±20% 이내: 양호
            ratio in 0.7..1.3 -> 0.6      // ±30% 이내: 보통
            ratio in 0.6..1.4 -> 0.4      // ±40% 이내: 미흡
            else -> 0.2                    // 그 외: 불량
        }
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 무음 여부 판단
     */
    private fun isSilence(pitch: Float): Boolean {
        return pitch < silenceThreshold
    }

    /**
     * RMS (Root Mean Square) 계산
     */
    private fun calculateRMS(data: List<Float>): Double {
        if (data.isEmpty()) return 0.0
        val sumSquares = data.sumOf { (it * it).toDouble() }
        return sqrt(sumSquares / data.size)
    }

    // ==================== 상세 분석 메서드 (디버깅/피드백용) ====================

    /**
     * 구간별 상세 점수 계산 (추후 피드백 기능에 활용)
     */
    fun getDetailedScores(): Map<String, Double> {
        return mapOf(
            "pitch_accuracy" to calculatePitchAccuracyWithDTW(),
            "rhythm_score" to calculateRhythmScore(),
            "volume_stability" to calculateVolumeStability(),
            "duration_match" to calculateDurationMatch()
        )
    }

    /**
     * 디버그 정보 반환
     */
    fun getDebugInfo(): String {
        val details = getDetailedScores()
        val vibratoInfo = detectVibrato()
        val difficulty = calculateDifficultyLevel()

        return """
            |===== Score Analysis Debug Info =====
            |Original Length: ${pitchListOriginal.size}
            |User Length: ${pitchListUser.size}
            |Pitch Accuracy: ${String.format("%.2f%%", details["pitch_accuracy"]!! * 100)}
            |Rhythm Score: ${String.format("%.2f%%", details["rhythm_score"]!! * 100)}
            |Volume Stability: ${String.format("%.2f%%", details["volume_stability"]!! * 100)}
            |Duration Match: ${String.format("%.2f%%", details["duration_match"]!! * 100)}
            |Vibrato Score: ${String.format("%.2f", vibratoInfo.score)}
            |Difficulty Level: ${difficulty.level} (${String.format("%.1f", difficulty.multiplier)}x)
            |Total Score: ${calculateTotalScore()}
            |=====================================
        """.trimMargin()
    }

    // ==================== 중기 기능: 비브라토 감지 ====================

    /**
     * 비브라토 정보를 담는 데이터 클래스
     */
    data class VibratoInfo(
        val hasVibrato: Boolean,
        val frequency: Double,  // 비브라토 주파수 (Hz)
        val extent: Double,     // 비브라토 폭 (Hz)
        val score: Double       // 비브라토 점수 (0.0 ~ 1.0)
    )

    /**
     * 비브라토 감지 및 평가
     *
     * 비브라토: 음높이의 주기적인 변화
     * - 일반적으로 4-7Hz의 주파수로 진동
     * - 적절한 비브라토는 가점 요소
     */
    fun detectVibrato(): VibratoInfo {
        val voicePitches = pitchListUser.filter { !isSilence(it) }

        if (voicePitches.size < 20) {
            return VibratoInfo(false, 0.0, 0.0, 0.5)
        }

        // 연속된 피치 변화 분석
        val pitchChanges = mutableListOf<Float>()
        for (i in 1 until voicePitches.size) {
            pitchChanges.add(voicePitches[i] - voicePitches[i - 1])
        }

        // 피크 카운팅 (방향 전환 횟수)
        var directionChanges = 0
        var lastChange = 0f

        for (change in pitchChanges) {
            if ((change > 0 && lastChange < 0) || (change < 0 && lastChange > 0)) {
                directionChanges++
            }
            if (change != 0f) {
                lastChange = change
            }
        }

        // 비브라토 주파수 추정 (초당 방향 전환 횟수 / 2)
        val durationInSeconds = voicePitches.size * 0.1 // 100ms per sample
        val vibratoFrequency = if (durationInSeconds > 0) {
            (directionChanges / 2.0) / durationInSeconds
        } else {
            0.0
        }

        // 비브라토 폭 계산 (평균 변화량)
        val vibratoExtent = if (pitchChanges.isNotEmpty()) {
            pitchChanges.map { abs(it) }.average()
        } else {
            0.0
        }

        // 비브라토 평가
        val hasVibrato = vibratoFrequency in 3.0..8.0 && vibratoExtent in 5.0..30.0

        val vibratoScore = when {
            !hasVibrato -> 0.5 // 비브라토 없음 (중립)
            vibratoFrequency in 4.0..7.0 && vibratoExtent in 8.0..20.0 -> 1.0 // 이상적인 비브라토
            vibratoFrequency in 3.0..8.0 && vibratoExtent in 5.0..30.0 -> 0.8 // 양호한 비브라토
            else -> 0.6 // 부적절한 비브라토
        }

        return VibratoInfo(
            hasVibrato = hasVibrato,
            frequency = vibratoFrequency,
            extent = vibratoExtent,
            score = vibratoScore
        )
    }

    // ==================== 중기 기능: 난이도 조정 ====================

    /**
     * 곡 난이도 정보
     */
    data class DifficultyInfo(
        val level: String,           // 난이도 레벨 (쉬움, 보통, 어려움, 매우 어려움)
        val multiplier: Double,      // 난이도 배수
        val rangeInSemitones: Int,   // 음역대 범위 (반음 단위)
        val avgPitch: Double,        // 평균 피치
        val highestPitch: Float,     // 최고음
        val lowestPitch: Float       // 최저음
    )

    /**
     * 곡 난이도 계산
     *
     * 난이도 요소:
     * - 음역대 범위 (최고음 - 최저음)
     * - 평균 음높이
     * - 고음 구간 비율
     */
    fun calculateDifficultyLevel(): DifficultyInfo {
        val voicePitches = pitchListOriginal.filter { !isSilence(it) }

        if (voicePitches.isEmpty()) {
            return DifficultyInfo("보통", 1.0, 0, 0.0, 0f, 0f)
        }

        val highestPitch = voicePitches.maxOrNull() ?: 0f
        val lowestPitch = voicePitches.minOrNull() ?: 0f
        val avgPitch = voicePitches.average()

        // 음역대 범위를 반음 단위로 계산
        val rangeInSemitones = hzToSemitones(highestPitch, lowestPitch)

        // 고음 구간 비율 (평균보다 높은 음)
        val highNoteRatio = voicePitches.count { it > avgPitch }.toDouble() / voicePitches.size

        // 난이도 점수 계산 (0-100)
        val difficultyScore = calculateDifficultyScore(rangeInSemitones, avgPitch, highNoteRatio)

        // 난이도 레벨 및 배수 결정
        val (level, multiplier) = when {
            difficultyScore >= 80 -> "매우 어려움" to 1.3
            difficultyScore >= 60 -> "어려움" to 1.15
            difficultyScore >= 40 -> "보통" to 1.0
            difficultyScore >= 20 -> "쉬움" to 0.9
            else -> "매우 쉬움" to 0.85
        }

        return DifficultyInfo(
            level = level,
            multiplier = multiplier,
            rangeInSemitones = rangeInSemitones,
            avgPitch = avgPitch,
            highestPitch = highestPitch,
            lowestPitch = lowestPitch
        )
    }

    /**
     * Hz를 반음 단위로 변환 (음역대 계산용)
     */
    private fun hzToSemitones(freq1: Float, freq2: Float): Int {
        if (freq1 <= 0 || freq2 <= 0) return 0
        val ratio = max(freq1, freq2) / min(freq1, freq2)
        return (12 * kotlin.math.log2(ratio.toDouble())).toInt()
    }

    /**
     * 난이도 점수 계산 (0-100)
     */
    private fun calculateDifficultyScore(
        rangeInSemitones: Int,
        avgPitch: Double,
        highNoteRatio: Double
    ): Int {
        // 음역대 점수 (24반음 = 2옥타브 이상이면 어려움)
        val rangeScore = (rangeInSemitones / 24.0 * 50).coerceIn(0.0, 50.0)

        // 평균 음높이 점수 (350Hz 이상이면 높은 곡)
        val pitchScore = when {
            avgPitch >= 400 -> 30.0
            avgPitch >= 350 -> 20.0
            avgPitch >= 300 -> 10.0
            else -> 5.0
        }

        // 고음 비율 점수
        val highNoteScore = (highNoteRatio * 20).coerceIn(0.0, 20.0)

        return (rangeScore + pitchScore + highNoteScore).toInt()
    }

    /**
     * 난이도를 반영한 조정된 점수 계산
     */
    fun calculateAdjustedScore(): Int {
        val baseScore = calculateTotalScore()
        val difficulty = calculateDifficultyLevel()

        // 난이도에 따른 보너스/페널티 적용
        val adjustedScore = (baseScore * difficulty.multiplier).toInt()

        return adjustedScore.coerceIn(0, 100)
    }

    // ==================== 중기 기능: 구간별 피드백 ====================

    /**
     * 구간별 점수 및 피드백 정보
     */
    data class SectionScore(
        val sectionIndex: Int,
        val startTime: Int,         // 시작 시간 (100ms 단위)
        val endTime: Int,           // 종료 시간 (100ms 단위)
        val pitchAccuracy: Double,  // 음정 정확도
        val rhythmAccuracy: Double, // 리듬 정확도
        val totalScore: Double,     // 구간 점수
        val feedback: String,       // 피드백 메시지
        val quality: ScoreQuality   // 점수 등급
    )

    /**
     * 점수 등급
     */
    enum class ScoreQuality {
        EXCELLENT,  // 90-100
        GOOD,       // 80-89
        FAIR,       // 70-79
        POOR,       // 60-69
        BAD         // 0-59
    }

    /**
     * 구간별 상세 점수 계산
     * @param sectionLength 구간 길이 (기본 50 = 5초)
     */
    fun calculateSectionScores(sectionLength: Int = 50): List<SectionScore> {
        if (pitchListOriginal.isEmpty() || pitchListUser.isEmpty()) {
            return emptyList()
        }

        val sections = mutableListOf<SectionScore>()
        val totalLength = minOf(pitchListOriginal.size, pitchListUser.size)

        var sectionIndex = 0
        var startIdx = 0

        while (startIdx < totalLength) {
            val endIdx = minOf(startIdx + sectionLength, totalLength)

            // 구간 데이터 추출
            val origSection = pitchListOriginal.subList(startIdx, endIdx)
            val userSection = pitchListUser.subList(startIdx, endIdx)

            // 구간별 점수 계산
            val sectionAnalyzer = ScoreAnalyzer(origSection, userSection, toleranceHz, silenceThreshold)

            val pitchAcc = sectionAnalyzer.calculatePitchAccuracyWithDTW()
            val rhythmAcc = sectionAnalyzer.calculateRhythmScore()
            val sectionScore = (pitchAcc * 0.7 + rhythmAcc * 0.3) * 100

            // 피드백 생성
            val feedback = generateSectionFeedback(pitchAcc, rhythmAcc, sectionScore)
            val quality = determineQuality(sectionScore)

            sections.add(
                SectionScore(
                    sectionIndex = sectionIndex,
                    startTime = startIdx,
                    endTime = endIdx,
                    pitchAccuracy = pitchAcc,
                    rhythmAccuracy = rhythmAcc,
                    totalScore = sectionScore,
                    feedback = feedback,
                    quality = quality
                )
            )

            sectionIndex++
            startIdx = endIdx
        }

        return sections
    }

    /**
     * 구간별 피드백 메시지 생성
     */
    private fun generateSectionFeedback(
        pitchAccuracy: Double,
        rhythmAccuracy: Double,
        score: Double
    ): String {
        return when {
            score >= 90 -> "완벽해요! 이 구간을 매우 잘 불렀습니다."
            score >= 80 -> when {
                pitchAccuracy < 0.8 -> "음정을 조금 더 정확하게 맞춰보세요."
                rhythmAccuracy < 0.8 -> "박자를 조금 더 정확하게 맞춰보세요."
                else -> "잘 부르셨어요!"
            }
            score >= 70 -> when {
                pitchAccuracy < rhythmAccuracy -> "음정에 더 집중해보세요."
                else -> "박자를 더 정확하게 맞춰보세요."
            }
            score >= 60 -> "이 구간을 다시 연습해보세요."
            else -> "많은 연습이 필요한 구간입니다."
        }
    }

    /**
     * 점수 등급 결정
     */
    private fun determineQuality(score: Double): ScoreQuality {
        return when {
            score >= 90 -> ScoreQuality.EXCELLENT
            score >= 80 -> ScoreQuality.GOOD
            score >= 70 -> ScoreQuality.FAIR
            score >= 60 -> ScoreQuality.POOR
            else -> ScoreQuality.BAD
        }
    }

    /**
     * 구간별 피드백 요약
     */
    fun getSectionFeedbackSummary(sectionLength: Int = 50): String {
        val sections = calculateSectionScores(sectionLength)

        if (sections.isEmpty()) {
            return "분석할 데이터가 부족합니다."
        }

        val excellent = sections.count { it.quality == ScoreQuality.EXCELLENT }
        val good = sections.count { it.quality == ScoreQuality.GOOD }
        val needsImprovement = sections.count { it.quality == ScoreQuality.POOR || it.quality == ScoreQuality.BAD }

        val weakSections = sections
            .filter { it.totalScore < 70 }
            .sortedBy { it.totalScore }
            .take(3)

        val summary = StringBuilder()
        summary.appendLine("===== 구간별 분석 요약 =====")
        summary.appendLine("전체 구간: ${sections.size}개")
        summary.appendLine("우수: ${excellent}개 | 양호: ${good}개 | 개선 필요: ${needsImprovement}개")

        if (weakSections.isNotEmpty()) {
            summary.appendLine("\n개선이 필요한 구간:")
            weakSections.forEach { section ->
                val timeStart = String.format("%.1f", section.startTime * 0.1)
                val timeEnd = String.format("%.1f", section.endTime * 0.1)
                summary.appendLine("  ${timeStart}s-${timeEnd}s: ${String.format("%.0f", section.totalScore)}점 - ${section.feedback}")
            }
        }

        summary.appendLine("==========================")

        return summary.toString()
    }
}
