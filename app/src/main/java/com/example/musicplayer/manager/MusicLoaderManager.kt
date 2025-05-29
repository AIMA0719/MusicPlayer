package com.example.musicplayer.manager

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.musicplayer.listObjects.MusicItem
import java.io.File

object MusicLoaderManager {

    fun loadAudioList(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()

        // 1. 외부 저장소에서 mp4, wav, mp3, m4a 파일 가져오기 (MediaStore)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.MIME_TYPE} IN (?, ?, ?, ?)"
        val selectionArgs = arrayOf("audio/mp4", "audio/wav", "audio/mpeg", "audio/x-m4a")

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musicList.add(
                    MusicItem(
                        id = id.toString(),
                        uri = contentUri.toString(),
                        fileName = name,
                        filePath = contentUri.toString()
                    )
                )
            }
        }

        /*// 2. 내부 저장소에서 mp4, wav, mp3, m4a 파일 가져오기
        val internalStorageDir = Environment.getExternalStorageDirectory()
        searchAudioFilesRecursively(internalStorageDir, musicList)*/

        // 총 파일 개수 출력
        Log.d("MusicLoaderManager", "Total audio files found: ${musicList.size}")
        return musicList
    }

    // 내부 저장소에서 오디오 파일을 재귀적으로 탐색하는 함수
    private fun searchAudioFilesRecursively(dir: File, audioFiles: MutableList<MusicItem>) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    searchAudioFilesRecursively(file, audioFiles)
                } else if (isAudioFile(file)) {
                    audioFiles.add(
                        MusicItem(
                            id = file.name,
                            uri = file.toURI().toString(),
                            fileName = file.name,
                            filePath = file.absolutePath
                        )
                    )
                }
            }
        }
    }

    // 파일 확장자가 mp4, wav, mp3, m4a인지 확인하는 함수
    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = listOf("mp4", "wav", "mp3", "m4a")
        return audioExtensions.any { file.extension.equals(it, ignoreCase = true) }
    }
}
