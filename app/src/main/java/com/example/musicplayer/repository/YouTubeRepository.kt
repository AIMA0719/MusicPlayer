package com.example.musicplayer.repository

import android.content.Context
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.server.RetrofitClient
import com.example.musicplayer.server.YouTubeApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class YouTubeRepository(private val context: Context) {

    private val youTubeApiService: YouTubeApiService by lazy {
        RetrofitClient.createYouTubeService {
            // This is a blocking call, but Interceptor runs on background thread usually.
            // However, we need to be careful. Ideally we pre-fetch or use runBlocking if needed,
            // but since we are in a suspend function context usually, we can't easily call suspend from interceptor.
            // For simplicity in this architecture, we'll try to get the token synchronously or assume it's valid.
            // A better approach is to pass the token in, but the interceptor is cleaner.
            // We will use a small hack: runBlocking is bad, but GoogleAuthUtil.getToken is blocking anyway.
            // So we can just call it.
            val account = GoogleAuthManager.getLastSignedInAccount(context)
            if (account != null) {
                try {
                    // GoogleAuthUtil.getToken must be called on background thread.
                    // OkHttp Interceptor runs on background thread.
                    val scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly"
                    com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account.account!!, scope)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun getMyPlaylists(): Response<JsonObject> {
        return withContext(Dispatchers.IO) {
            youTubeApiService.getMyPlaylists()
        }
    }

    suspend fun getPlaylistItems(playlistId: String): Response<JsonObject> {
        return withContext(Dispatchers.IO) {
            youTubeApiService.getPlaylistItems(playlistId = playlistId)
        }
    }
    
    suspend fun searchVideos(query: String): Response<JsonObject> {
        return withContext(Dispatchers.IO) {
            youTubeApiService.searchVideos(query = query)
        }
    }
}
