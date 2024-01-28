package com.example.musicplayer.Manager

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musicplayer.placeholder.PlaceholderContent

object MusicLoaderManager {
    fun loadMusic(context: Context): List<PlaceholderContent.PlaceholderItem> {
        val musicList = mutableListOf<PlaceholderContent.PlaceholderItem>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? OR " +
                "${MediaStore.Audio.Media.DATA} like ? "

        val selectionArgs = arrayOf("%mp3%", "%m4a%", "%flac%", "%wav%")
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musicList.add(
                    PlaceholderContent.PlaceholderItem(
                        contentUri.toString(),
                        name,
                        data
                    )
                )
            }
        }

        return musicList
    }
}