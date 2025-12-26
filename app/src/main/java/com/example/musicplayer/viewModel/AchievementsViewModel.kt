package com.example.musicplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.AchievementEntity
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AchievementsFragment ViewModel
 */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        // 기존 수집 작업 취소 후 새로 시작
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            achievementRepository.getAllByUser(userId).collectLatest { entities ->
                val unlockedCount = entities.count { it.isUnlocked }
                val totalCount = Achievement.entries.size

                // Achievement와 Entity를 매칭
                val displayList = Achievement.entries.map { achievement ->
                    val entity = entities.find { it.achievementId == achievement.id }
                    AchievementDisplayModel(achievement, entity)
                }

                _uiState.update {
                    it.copy(
                        achievements = displayList,
                        unlockedCount = unlockedCount,
                        totalCount = totalCount,
                        isLoading = false
                    )
                }
            }
        }
    }
}

/**
 * Achievements UI State
 */
data class AchievementsUiState(
    val achievements: List<AchievementDisplayModel> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * Achievement Display Model
 * Achievement 정의와 사용자의 진행 상황을 함께 표시
 */
data class AchievementDisplayModel(
    val achievement: Achievement,
    val entity: AchievementEntity?
)
