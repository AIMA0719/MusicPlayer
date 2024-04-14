package com.example.musicplayer

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitService {
    @GET("posts/{postId}")
    fun getPosts(@Path("postId") postId: String): Call<PostResult>
}
