package com.example.musicplayer.Manager

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.musicplayer.ListObjects.MusicItem
import java.io.File

// 음악 파일 정보를 로드하는 Manager 클래스
object MusicLoaderManager {

    // 음악 파일을 로드하여 PlaceholderItem 리스트로 반환하는 함수
    fun loadMusic(context: Context): List<MusicItem> {
        // 음악 정보를 저장할 리스트 초기화
        val musicList = mutableListOf<MusicItem>()

        // 음악 파일 정보를 가져오기 위한 열(Column) 정의
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, // 음악 파일의 고유 ID
            MediaStore.Audio.Media.DISPLAY_NAME, // 음악 파일의 표시 이름
            MediaStore.Audio.Media.DATA // 음악 파일의 경로
        )

        val selection = "${MediaStore.Audio.Media.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("audio/%")

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        // ContentResolver를 사용하여 미디어 파일 쿼리 실행
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // 외부 저장소의 오디오 파일 URI
            projection, // 반환할 열 정의
            selection, // 검색 조건
            selectionArgs, // 검색 조건 값
            sortOrder // 정렬 기준
        )?.use { cursor -> // 쿼리 결과를 Cursor로 반환
            // Cursor에서 열 인덱스 가져오기
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            // Cursor를 통해 검색된 데이터를 순회
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn) // 음악 파일 ID 가져오기
                val name = cursor.getString(nameColumn) // 음악 파일 이름 가져오기
                val data = cursor.getString(dataColumn) // 음악 파일 경로 가져오기

                // 음악 파일 URI 생성
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // PlaceholderItem 객체 생성 후 리스트에 추가
                musicList.add(
                    MusicItem(
                        id.toString(),
                        contentUri.toString(), // URI를 문자열로 저장
                        name, // 음악 파일 이름 저장
                        data // 음악 파일 경로 저장
                    )
                )
            }
        }

        // 생성된 음악 리스트 반환
        return musicList
    }

    fun loadAllAudioFiles(context: Context): List<MusicItem> {
        val audioFiles = mutableListOf<MusicItem>()

        // 내장 저장소 디렉터리 경로 가져오기
        val internalStorageDir = context.filesDir // 앱 내장 디렉터리
        val externalStorageDir = Environment.getExternalStorageDirectory() // 외부 내장 디렉터리

        // 디렉터리 탐색하여 파일 찾기
        internalStorageDir?.let { searchFilesRecursively(it, audioFiles) }
        externalStorageDir?.let { searchFilesRecursively(it, audioFiles) }

        return audioFiles
    }

    // 재귀적으로 디렉터리를 탐색하며 파일을 추가하는 함수
    private fun searchFilesRecursively(dir: File, audioFiles: MutableList<MusicItem>) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    // 디렉터리면 재귀 호출
                    searchFilesRecursively(file, audioFiles)
                } else if (isAudioFile(file)) {
                    // 음성 파일이면 MusicItem 객체로 리스트에 추가
                    audioFiles.add(
                        MusicItem(
                            id = file.name,           // 파일 이름을 고유 ID로 사용
                            uri = file.toURI().toString(), // 파일 URI
                            filePath = file.absolutePath, // 파일 절대 경로
                            fileName = file.name      // 파일 이름
                        )
                    )
                }
            }
        }
    }

    // 파일 확장자가 음성 파일인지 확인하는 함수
    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = listOf("mp3", "wav", "m4a", "amr", "flac")
        return audioExtensions.any { file.extension.equals(it, ignoreCase = true) }
    }
}
