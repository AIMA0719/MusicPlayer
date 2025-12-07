package com.example.musicplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.RecordingHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 통계 ViewModel
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val recordingHistoryRepository: RecordingHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"
        android.util.Log.d("StatisticsVM", "Loading statistics for userId: $userId")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // 기본 통계
                val totalCount = recordingHistoryRepository.getTotalRecordingCount(userId)
                val averageScore = recordingHistoryRepository.getAverageScore(userId)
                val highestScore = recordingHistoryRepository.getHighestScore(userId)
                val uniqueSongCount = recordingHistoryRepository.getUniqueSongCount(userId)

                // 점수 분포 계산
                val scoreDistribution = calculateScoreDistribution(userId)

                // 최근 7일 녹음 트렌드
                val weeklyTrend = calculateWeeklyTrend(userId)

                // 난이도별 통계
                val difficultyStats = calculateDifficultyStats(userId)

                // 최근 10개 녹음의 점수 추이
                val recentScores = recordingHistoryRepository.getRecentRecordings(userId, 10)
                    .map { it.totalScore }
                    .reversed() // 오래된 것부터 표시

                _uiState.update {
                    it.copy(
                        totalRecordings = totalCount,
                        averageScore = averageScore,
                        highestScore = highestScore,
                        uniqueSongs = uniqueSongCount,
                        scoreDistribution = scoreDistribution,
                        weeklyTrend = weeklyTrend,
                        difficultyStats = difficultyStats,
                        recentScoreTrend = recentScores,
                        isLoading = false,
                        isEmpty = totalCount == 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun calculateScoreDistribution(userId: String): ScoreDistribution {
        val excellent = recordingHistoryRepository.getCountByMinScore(userId, 90) // 90점 이상
        val good = recordingHistoryRepository.getCountByScoreRange(userId, 70, 90) // 70-89점
        val average = recordingHistoryRepository.getCountByScoreRange(userId, 50, 70) // 50-69점
        val needsWork = recordingHistoryRepository.getCountByScoreRange(userId, 0, 50) // 50점 미만

        return ScoreDistribution(
            excellent = excellent,
            good = good,
            average = average,
            needsWork = needsWork
        )
    }

    private suspend fun calculateWeeklyTrend(userId: String): List<DailyRecordingCount> {
        val result = mutableListOf<DailyRecordingCount>()
        val calendar = Calendar.getInstance()

        // 오늘부터 7일 전까지
        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis

            val count = recordingHistoryRepository.getRecordingCountInTimeRange(userId, startOfDay, endOfDay)

            val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
            calendar.timeInMillis = startOfDay
            val dayOfWeek = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]

            result.add(DailyRecordingCount(dayOfWeek, count))
        }

        return result
    }

    private suspend fun calculateDifficultyStats(userId: String): List<DifficultyStatItem> {
        val difficulties = listOf(
            "VERY_EASY" to "매우 쉬움",
            "EASY" to "쉬움",
            "NORMAL" to "보통",
            "HARD" to "어려움",
            "VERY_HARD" to "매우 어려움"
        )

        val result = mutableListOf<DifficultyStatItem>()

        for ((key, displayName) in difficulties) {
            val recordings = recordingHistoryRepository.getRecentRecordings(userId, 1000)
                .filter { it.difficulty == key }

            if (recordings.isNotEmpty()) {
                val count = recordings.size
                val avgScore = recordings.map { it.totalScore }.average()
                result.add(DifficultyStatItem(displayName, count, avgScore))
            }
        }

        return result
    }
}

/**
 * UI State
 */
data class StatisticsUiState(
    val totalRecordings: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Int = 0,
    val uniqueSongs: Int = 0,
    val scoreDistribution: ScoreDistribution = ScoreDistribution(),
    val weeklyTrend: List<DailyRecordingCount> = emptyList(),
    val difficultyStats: List<DifficultyStatItem> = emptyList(),
    val recentScoreTrend: List<Int> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val error: String? = null
)

/**
 * 점수 분포
 */
data class ScoreDistribution(
    val excellent: Int = 0,  // 90점 이상
    val good: Int = 0,       // 70-89점
    val average: Int = 0,    // 50-69점
    val needsWork: Int = 0   // 50점 미만
) {
    val total: Int get() = excellent + good + average + needsWork

    fun excellentPercentage(): Float = if (total > 0) excellent * 100f / total else 0f
    fun goodPercentage(): Float = if (total > 0) good * 100f / total else 0f
    fun averagePercentage(): Float = if (total > 0) average * 100f / total else 0f
    fun needsWorkPercentage(): Float = if (total > 0) needsWork * 100f / total else 0f
}

/**
 * 일별 녹음 수
 */
data class DailyRecordingCount(
    val dayName: String,
    val count: Int
)

/**
 * 난이도별 통계
 */
data class DifficultyStatItem(
    val difficultyName: String,
    val count: Int,
    val averageScore: Double
)
