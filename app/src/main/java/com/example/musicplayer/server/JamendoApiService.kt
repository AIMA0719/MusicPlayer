package com.example.musicplayer.server

import com.example.musicplayer.server.data.JamendoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {

    /**
     * Search for tracks on Jamendo
     * @param clientId - Required API client ID
     * @param format - Response format (json, jsonpretty, xml)
     * @param limit - Max results (default 10, max 200)
     * @param offset - Pagination offset
     * @param order - Sort order (popularity_month, downloads_month, etc.)
     * @param search - Free text search
     * @param audioDownloadFormat - Download format (mp31, mp32, ogg, flac)
     * @param imageSize - Image size in pixels (25-600)
     * @param include - Additional fields (musicinfo, stats, licenses)
     */
    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String = "popularity_month",
        @Query("search") search: String? = null,
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 200,
        @Query("include") include: String = "musicinfo"
    ): Response<JamendoResponse>

    /**
     * Get featured tracks
     */
    @GET("tracks")
    suspend fun getFeaturedTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("featured") featured: String = "1",
        @Query("order") order: String = "popularity_month",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 200,
        @Query("include") include: String = "musicinfo"
    ): Response<JamendoResponse>

    /**
     * Get tracks by tags/genre
     */
    @GET("tracks")
    suspend fun getTracksByTags(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("tags") tags: String,
        @Query("order") order: String = "popularity_month",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 200,
        @Query("include") include: String = "musicinfo"
    ): Response<JamendoResponse>

    /**
     * Get tracks by specific artist
     */
    @GET("tracks")
    suspend fun getTracksByArtist(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("artist_id") artistId: String,
        @Query("order") order: String = "releasedate_desc",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 200,
        @Query("include") include: String = "musicinfo"
    ): Response<JamendoResponse>
}
