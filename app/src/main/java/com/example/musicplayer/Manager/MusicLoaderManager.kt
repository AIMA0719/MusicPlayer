package com.example.musicplayer.Manager

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musicplayer.ListObjects.MusicItem

// 음악 파일 정보를 로드하는 Manager 클래스
object MusicLoaderManager {

    // 음악 파일을 로드하여 PlaceholderItem 리스트로 반환하는 함수
    fun loadMusic(context: Context): List<MusicItem.MusicItem> {
        // 음악 정보를 저장할 리스트 초기화
        val musicList = mutableListOf<MusicItem.MusicItem>()

        // 음악 파일 정보를 가져오기 위한 열(Column) 정의
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, // 음악 파일의 고유 ID
            MediaStore.Audio.Media.DISPLAY_NAME, // 음악 파일의 표시 이름
            MediaStore.Audio.Media.DATA // 음악 파일의 경로
        )

        // 검색 조건 정의 - 특정 확장자를 가진 음악 파일만 검색
        val selection = "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? "

        // 검색 조건에 전달할 확장자 배열
        val selectionArgs = arrayOf("%mp3%", "%m4a%", "%flac%", "%wav%")

        // 검색 결과를 정렬할 기준
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

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
                    MusicItem.MusicItem(
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
}
