package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.JamendoTrack
import com.example.musicplayer.domain.model.MusicTrack
import kotlinx.coroutines.flow.Flow

/**
 * 음악 검색 및 스트리밍 Repository 인터페이스
 */
interface MusicRepository {
    /**
     * 음악 검색 (Jamendo API)
     */
    suspend fun searchMusic(query: String, limit: Int = 20): Result<List<JamendoTrack>>

    /**
     * 인기 음악 조회
     */
    suspend fun getPopularMusic(limit: Int = 20): Result<List<JamendoTrack>>

    /**
     * 최신 음악 조회
     */
    suspend fun getLatestMusic(limit: Int = 20): Result<List<JamendoTrack>>

    /**
     * 로컬 음악 목록 조회
     */
    fun getLocalMusic(): Flow<List<MusicTrack>>

    /**
     * 다운로드된 음악 목록 조회
     */
    fun getDownloadedMusic(): Flow<List<MusicTrack>>
}
