package com.example.musicplayer.server

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    /**
     * Get playlists from the user's channel.
     * We specifically look for the "LL" (Liked) playlist or similar if needed.
     * For now, let's fetch the user's playlists.
     */
    @GET("playlists")
    suspend fun getMyPlaylists(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("mine") mine: Boolean = true,
        @Query("maxResults") maxResults: Int = 50
    ): Response<JsonObject>

    /**
     * Get items (videos) from a specific playlist.
     */
    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50
    ): Response<JsonObject>
    
    /**
     * Search for videos (alternative to getting liked videos if that is restricted)
     */
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20
    ): Response<JsonObject>
}
