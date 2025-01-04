package com.example.musicplayer.Manager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

object UriUtils {
    fun getMediaFilePath(context: Context, contentUri: String): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(
            Uri.parse(contentUri),
            projection, null, null, null
        )
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        var filePath: String? = null
        cursor?.use {
            if (it.moveToFirst()) {
                filePath = it.getString(columnIndex!!)
            }
        }
        return filePath
    }

    fun copyUriToTempFile(uri: Uri, context: Context): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw RuntimeException("Unable to open URI")
        val tempFile = File(context.cacheDir, "temp_audio_file.mp4")
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile.absolutePath
    }

}

