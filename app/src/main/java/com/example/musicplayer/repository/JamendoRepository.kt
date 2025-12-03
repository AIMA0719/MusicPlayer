package com.example.musicplayer.repository

import com.example.musicplayer.server.JamendoApiService
import com.example.musicplayer.server.data.JamendoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JamendoRepository @Inject constructor(
    private val jamendoApiService: JamendoApiService
) {
    // Jamendo API Client ID
    // Note: In production, this should be stored securely (e.g., in BuildConfig or secrets)
    // todo Jamendo API KEY
    private val clientId = "56d30c95"

    /**
     * Search for tracks with a query string
     */
    suspend fun searchTracks(
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): Response<JamendoResponse> {
        return withContext(Dispatchers.IO) {
            jamendoApiService.searchTracks(
                clientId = clientId,
                search = query,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Get featured tracks selected by Jamendo
     */
    suspend fun getFeaturedTracks(
        limit: Int = 50,
        offset: Int = 0
    ): Response<JamendoResponse> {
        return withContext(Dispatchers.IO) {
            jamendoApiService.getFeaturedTracks(
                clientId = clientId,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Get tracks by tags/genre
     * @param tags - Tags to filter by (e.g., "pop", "rock", "electronic")
     */
    suspend fun getTracksByTags(
        tags: String,
        limit: Int = 50,
        offset: Int = 0
    ): Response<JamendoResponse> {
        return withContext(Dispatchers.IO) {
            jamendoApiService.getTracksByTags(
                clientId = clientId,
                tags = tags,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Get tracks by a specific artist
     */
    suspend fun getTracksByArtist(
        artistId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Response<JamendoResponse> {
        return withContext(Dispatchers.IO) {
            jamendoApiService.getTracksByArtist(
                clientId = clientId,
                artistId = artistId,
                limit = limit,
                offset = offset
            )
        }
    }
}
