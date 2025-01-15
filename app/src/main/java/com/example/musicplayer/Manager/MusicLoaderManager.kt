package com.example.musicplayer.Manager

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicplayer.ListObjects.MusicItem
import java.io.File

object MusicLoaderManager {

    fun getRecordList(): List<MusicItem> {
        val filepath = Environment.getExternalStorageDirectory()
        val path = filepath.path // /storage/emulated/0
        val folderName = "Recordings"

        val directory = File("$path/$folderName")
        val files = directory.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        val musicList = mutableListOf<MusicItem>()
        for (file in files) {
            musicList.add(MusicItem(
                id = file.name,
                uri = file.toURI().toString(),
                fileName = file.name,
                filePath = file.absolutePath
            ))
        }

        return musicList
    }


    // 모든 음성 파일을 가져오는 함수 (내부 저장소 + 외부 저장소)
    fun loadAllAudioFiles(context: Context): List<MusicItem> {
        scanAudioFiles(context)  // 파일 강제 스캔
        val audioFiles = mutableListOf<MusicItem>()

        // 외부 저장소의 음성 파일 가져오기
        audioFiles.addAll(loadAudioFilesFromMediaStore(context))

        // 내부 저장소의 음성 파일 가져오기 (앱 전용 디렉터리 탐색)
        val internalStorageDir = context.filesDir
        searchAudioFilesRecursively(internalStorageDir, audioFiles)

        return audioFiles
    }


    // MediaStore를 통해 외부 저장소에서 오디오 파일을 가져오는 함수
    private fun loadAudioFilesFromMediaStore(context: Context): List<MusicItem> {
        val musicList = mutableListOf<MusicItem>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            Log.d("MusicLoader", "Total audio files found: ${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musicList.add(
                    MusicItem(
                        id = id.toString(),
                        uri = contentUri.toString(),
                        fileName = name,
                        filePath = data
                    )
                )
            }
        }

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

    // 파일 확장자가 오디오 파일인지 확인하는 함수
    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = listOf("mp3", "wav", "m4a", "amr", "aac", "ogg", "flac")
        return audioExtensions.any { file.extension.equals(it, ignoreCase = true) }
    }

    // 특정 디렉터리를 미디어 스캐너로 강제 스캔하는 함수
    fun scanAudioFiles(context: Context) {
        val audioDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val files = audioDir.listFiles() ?: return

        val filePaths = files.filter { it.isFile && isAudioFile(it) }
            .map { it.absolutePath }
            .toTypedArray()

        MediaScannerConnection.scanFile(context, filePaths, null) { path, uri ->
            Log.d("MediaScanner", "Scanned: $path -> Uri: $uri")
        }
    }
}
