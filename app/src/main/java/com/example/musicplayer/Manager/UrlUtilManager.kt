package com.example.musicplayer.Manager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object UriUtils {
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null

        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT -> {
                filePath = getRealPathFromUriBelowKitkat(context, uri)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                filePath = getRealPathFromUriKitkatToMarshmallow(context, uri)
            }
            else -> {
                filePath = getRealPathFromUriAboveMarshmallow(context, uri)
            }
        }

        return filePath
    }

    private fun getRealPathFromUriBelowKitkat(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        var columnIndex = 0
        if (cursor != null) {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
    }

    private fun getRealPathFromUriKitkatToMarshmallow(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        var columnIndex = 0
        if (cursor != null) {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
    }

    private fun getRealPathFromUriAboveMarshmallow(context: Context, uri: Uri): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor == null) {
            return uri.path
        } else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            val filePath = cursor.getString(index)
            cursor.close()
            return filePath
        }
    }

    fun getMediaFilePath(context: Context, contentUri: String): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(
            android.net.Uri.parse(contentUri),
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
}

