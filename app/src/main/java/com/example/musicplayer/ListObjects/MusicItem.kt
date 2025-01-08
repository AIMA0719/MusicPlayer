package com.example.musicplayer.ListObjects

import java.io.File

// MusicItem 데이터 클래스 정의
data class MusicItem(
    val id: String,        // 파일 ID (고유 식별자)
    val uri: String,       // 파일 URI
    val filePath: String,  // 파일 경로
    val fileName: String   // 파일 이름
) {
    // MusicItem의 문자열 표현을 파일 이름으로 반환
    override fun toString(): String = fileName
}
