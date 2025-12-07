package com.example.musicplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.RecordingHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 녹음 기록 ViewModel
 */
@HiltViewModel
class RecordingHistoryViewModel @Inject constructor(
    private val recordingHistoryRepository: RecordingHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingHistoryUiState())
    val uiState: StateFlow<RecordingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecordingHistory()
    }

    fun loadRecordingHistory() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"
        android.util.Log.d("RecordingHistoryVM", "Loading history for userId: $userId")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            recordingHistoryRepository.getAllByUser(userId).collect { recordings ->
                android.util.Log.d("RecordingHistoryVM", "Found ${recordings.size} recordings for userId: $userId")
                // 날짜별로 그룹화
                val grouped = recordings.groupBy { recording ->
                    SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(Date(recording.timestamp))
                }

                val displayList = mutableListOf<RecordingHistoryItem>()
                grouped.forEach { (date, items) ->
                    displayList.add(RecordingHistoryItem.DateHeader(date))
                    items.forEach { recording ->
                        displayList.add(RecordingHistoryItem.Recording(recording))
                    }
                }

                // 통계 계산
                val stats = if (recordings.isNotEmpty()) {
                    RecordingStats(
                        totalCount = recordings.size,
                        averageScore = recordings.map { it.totalScore }.average(),
                        highestScore = recordings.maxOfOrNull { it.totalScore } ?: 0,
                        uniqueSongs = recordings.map { it.songName }.distinct().size
                    )
                } else {
                    RecordingStats()
                }

                _uiState.update {
                    it.copy(
                        recordings = displayList,
                        stats = stats,
                        isLoading = false,
                        isEmpty = recordings.isEmpty()
                    )
                }
            }
        }
    }

    fun deleteRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingHistoryRepository.deleteById(recordingId)
        }
    }

    fun getRecordingFilePath(recordingId: Long, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val recording = recordingHistoryRepository.getById(recordingId)
            callback(recording?.recordingFilePath)
        }
    }
}

/**
 * UI State
 */
data class RecordingHistoryUiState(
    val recordings: List<RecordingHistoryItem> = emptyList(),
    val stats: RecordingStats = RecordingStats(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

/**
 * 녹음 통계
 */
data class RecordingStats(
    val totalCount: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Int = 0,
    val uniqueSongs: Int = 0
)

/**
 * RecyclerView 아이템 (날짜 헤더 + 녹음 기록)
 */
sealed class RecordingHistoryItem {
    data class DateHeader(val date: String) : RecordingHistoryItem()
    data class Recording(val entity: RecordingHistoryEntity) : RecordingHistoryItem()
}
