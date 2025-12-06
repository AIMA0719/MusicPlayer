package com.example.musicplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.AchievementEntity
import com.example.musicplayer.entity.LevelSystem
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.entity.UserLevelEntity
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.AchievementRepository
import com.example.musicplayer.repository.ScoreRepository
import com.example.musicplayer.repository.UserLevelRepository
import com.example.musicplayer.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * MainFragment ViewModel
 */
@HiltViewModel
class MainFragmentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userLevelRepository: UserLevelRepository,
    private val achievementRepository: AchievementRepository,
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        viewModelScope.launch {
            // 사용자 정보 로드
            val user = userRepository.getUserById(userId)
            _uiState.update { it.copy(user = user) }

            // 레벨 정보 로드
            val userLevel = userLevelRepository.getByUserIdSync(userId)
            _uiState.update { it.copy(userLevel = userLevel) }

            // 도전과제 정보 로드
            achievementRepository.getAllByUser(userId).collect { achievements ->
                val unlockedCount = achievements.count { it.isUnlocked }
                val totalCount = Achievement.entries.size
                val recentUnlocked = achievements
                    .filter { it.isUnlocked }
                    .sortedByDescending { it.unlockedAt }
                    .take(3)

                _uiState.update {
                    it.copy(
                        achievements = achievements,
                        unlockedAchievementCount = unlockedCount,
                        totalAchievementCount = totalCount,
                        recentUnlockedAchievements = recentUnlocked
                    )
                }
            }
        }

        viewModelScope.launch {
            loadScoreData(userId)
        }
    }

    private suspend fun loadScoreData(userId: String) {
        try {
            // 현재 월의 시작과 끝 타임스탬프 계산
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis

            // 월 평균 점수 조회
            val averageScore = scoreRepository.getMonthlyAverageScore(userId, startOfMonth, endOfMonth)

            // Top 3 점수 조회
            val top3Scores = scoreRepository.getMonthlyTop3Scores(userId, startOfMonth, endOfMonth)

            _uiState.update {
                it.copy(
                    monthlyAverageScore = averageScore?.toInt() ?: 0,
                    top3Scores = top3Scores
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLevelTitle(level: Int): String {
        return LevelSystem.getLevelTitle(level)
    }

    fun getRequiredExp(level: Int): Int {
        return LevelSystem.getRequiredExp(level)
    }
}

/**
 * MainFragment UI State
 */
data class MainUiState(
    val user: com.example.musicplayer.database.entity.User? = null,
    val userLevel: UserLevelEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val unlockedAchievementCount: Int = 0,
    val totalAchievementCount: Int = 0,
    val recentUnlockedAchievements: List<AchievementEntity> = emptyList(),
    val monthlyAverageScore: Int = 0,
    val top3Scores: List<ScoreEntity> = emptyList()
)