package com.example.musicplayer.server

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val RIOT_API_URL = "https://jsonplaceholder.typicode.com"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 시간 제한: 30초
        .readTimeout(30, TimeUnit.SECONDS) // 읽기 시간 제한: 30초
        .writeTimeout(30, TimeUnit.SECONDS) // 쓰기 시간 제한: 30초
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(RIOT_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}