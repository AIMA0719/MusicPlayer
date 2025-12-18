package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.FavoriteSong
import kotlinx.coroutines.flow.Flow

/**
 * 즐겨찾기 Repository 인터페이스
 */
interface FavoriteRepository {
    /**
     * 모든 즐겨찾기 조회
     */
    fun getAllFavorites(): Flow<List<FavoriteSong>>

    /**
     * 즐겨찾기 여부 확인
     */
    suspend fun isFavorite(songId: String): Boolean

    /**
     * 즐겨찾기 추가
     */
    suspend fun addFavorite(song: FavoriteSong)

    /**
     * 즐겨찾기 삭제
     */
    suspend fun removeFavorite(songId: String)

    /**
     * 즐겨찾기 토글
     */
    suspend fun toggleFavorite(song: FavoriteSong): Boolean
}
