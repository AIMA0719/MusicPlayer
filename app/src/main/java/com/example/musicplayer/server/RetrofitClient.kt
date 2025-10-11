package com.example.musicplayer.server

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Jamendo API Base URL
    private const val JAMENDO_API_URL = "https://api.jamendo.com/v3.0/"

    // Jamendo API Client ID (개인 앱 등록 필요, 테스트용으로 제공)
    // 실제 사용 시 https://developer.jamendo.com/ 에서 본인의 Client ID를 발급받으세요
    private const val JAMENDO_CLIENT_ID = "56d30c95"

    // API 키를 모든 요청에 자동으로 추가하는 인터셉터
    private val apiKeyInterceptor = Interceptor { chain ->
        val original = chain.request()
        val originalUrl = original.url

        // URL에 client_id 파라미터 추가
        val url = originalUrl.newBuilder()
            .addQueryParameter("client_id", JAMENDO_CLIENT_ID)
            .addQueryParameter("format", "json")
            .build()

        val request = original.newBuilder()
            .url(url)
            .build()

        chain.proceed(request)
    }

    // HTTP 클라이언트 설정
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 시간 제한: 30초
        .readTimeout(30, TimeUnit.SECONDS) // 읽기 시간 제한: 30초
        .writeTimeout(30, TimeUnit.SECONDS) // 쓰기 시간 제한: 30초
        .addInterceptor(apiKeyInterceptor) // API 키 인터셉터 추가
        .retryOnConnectionFailure(true) // 연결 실패 시 재시도
        .build()

    // Retrofit 인스턴스
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(JAMENDO_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    // API 서비스 인스턴스를 쉽게 가져오는 헬퍼 함수
    inline fun <reified T> createService(): T {
        return retrofit.create(T::class.java)
    }
}