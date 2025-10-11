package com.example.musicplayer.server

import com.example.musicplayer.server.model.JamendoTracksResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Jamendo API 서비스 인터페이스
 *
 * Jamendo API 문서: https://developer.jamendo.com/v3.0
 */
interface JamendoApiService {

    /**
     * 트랙 목록 가져오기
     *
     * @param limit 가져올 트랙 개수 (기본값: 10)
     * @param offset 페이징 오프셋 (기본값: 0)
     * @param order 정렬 기준 (popularity_week, popularity_month, popularity_total, releasedate, etc.)
     * @param audioFormat 오디오 포맷 (mp31, mp32, ogg)
     * @param include 포함할 추가 정보 (musicinfo, licenses, stats)
     * @param tags 장르 태그 필터링
     * @param search 검색 키워드
     */
    @GET("tracks/")
    suspend fun getTracks(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String = "popularity_week",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo+licenses",
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null
    ): Response<JamendoTracksResponse>

    /**
     * 특정 태그(장르)의 트랙 가져오기
     *
     * @param tags 장르 태그 (rock, pop, jazz, electronic, classical, etc.)
     * @param limit 가져올 트랙 개수
     * @param audioFormat 오디오 포맷
     * @param include 포함할 추가 정보
     */
    @GET("tracks/")
    suspend fun getTracksByGenre(
        @Query("tags") tags: String,
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "popularity_week",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo+licenses"
    ): Response<JamendoTracksResponse>

    /**
     * 검색어로 트랙 검색
     *
     * @param searchQuery 검색 키워드
     * @param limit 가져올 트랙 개수
     * @param audioFormat 오디오 포맷
     * @param include 포함할 추가 정보
     */
    @GET("tracks/")
    suspend fun searchTracks(
        @Query("search") searchQuery: String,
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "relevance",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo+licenses"
    ): Response<JamendoTracksResponse>

    /**
     * 파일 다운로드
     * Streaming 어노테이션으로 대용량 파일 다운로드 지원
     *
     * @param fileUrl 다운로드할 파일의 전체 URL
     */
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<okhttp3.ResponseBody>
}
