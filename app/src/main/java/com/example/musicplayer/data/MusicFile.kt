package com.example.musicplayer.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicFile(
    val uri: Uri,
    val title: String,
    val artist: String,
    val duration: Long
) : Parcelable
