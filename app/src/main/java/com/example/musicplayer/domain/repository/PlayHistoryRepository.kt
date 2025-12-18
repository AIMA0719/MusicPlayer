package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.PlayHistory
import kotlinx.coroutines.flow.Flow

/**
 * 재생 히스토리 Repository 인터페이스
 */
interface PlayHistoryRepository {
    /**
     * 모든 재생 히스토리 조회
     */
    fun getAllHistory(): Flow<List<PlayHistory>>

    /**
     * 최근 재생 히스토리 조회
     */
    fun getRecentHistory(limit: Int): Flow<List<PlayHistory>>

    /**
     * 재생 히스토리 추가
     */
    suspend fun addHistory(history: PlayHistory)

    /**
     * 재생 히스토리 삭제
     */
    suspend fun deleteHistory(id: Long)

    /**
     * 모든 재생 히스토리 삭제
     */
    suspend fun clearAllHistory()
}
