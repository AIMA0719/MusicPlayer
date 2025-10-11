package com.example.musicplayer.server.model

import com.google.gson.annotations.SerializedName

/**
 * Jamendo API 응답 모델
 */
data class JamendoTracksResponse(
    @SerializedName("headers")
    val headers: JamendoHeaders,

    @SerializedName("results")
    val results: List<JamendoTrack>
)

data class JamendoHeaders(
    @SerializedName("status")
    val status: String,

    @SerializedName("code")
    val code: Int,

    @SerializedName("error_message")
    val errorMessage: String?,

    @SerializedName("warnings")
    val warnings: String?,

    @SerializedName("results_count")
    val resultsCount: Int
)

data class JamendoTrack(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("duration")
    val duration: Int, // 초 단위

    @SerializedName("artist_id")
    val artistId: String,

    @SerializedName("artist_name")
    val artistName: String,

    @SerializedName("artist_idstr")
    val artistIdStr: String?,

    @SerializedName("album_name")
    val albumName: String,

    @SerializedName("album_id")
    val albumId: String,

    @SerializedName("license_ccurl")
    val licenseCcUrl: String?,

    @SerializedName("position")
    val position: Int,

    @SerializedName("releasedate")
    val releaseDate: String,

    @SerializedName("album_image")
    val albumImage: String?,

    @SerializedName("audio")
    val audio: String?, // 다운로드 URL (mp3)

    @SerializedName("audiodownload")
    val audioDownload: String?, // 다운로드 전용 URL

    @SerializedName("prourl")
    val proUrl: String?,

    @SerializedName("shorturl")
    val shortUrl: String?,

    @SerializedName("shareurl")
    val shareUrl: String?,

    @SerializedName("waveform")
    val waveform: String?,

    @SerializedName("image")
    val image: String?,

    @SerializedName("audiodownload_allowed")
    val audioDownloadAllowed: Boolean? = true,

    @SerializedName("musicinfo")
    val musicInfo: JamendoMusicInfo?
)

data class JamendoMusicInfo(
    @SerializedName("vocalinstrumental")
    val vocalInstrumental: String?,

    @SerializedName("lang")
    val lang: String?,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("speed")
    val speed: String?,

    @SerializedName("tags")
    val tags: JamendoTags?
)

data class JamendoTags(
    @SerializedName("genres")
    val genres: List<String>?,

    @SerializedName("instruments")
    val instruments: List<String>?,

    @SerializedName("vartags")
    val varTags: List<String>?
)

/**
 * UI에서 사용할 간소화된 트랙 모델로 변환하는 확장 함수
 */
fun JamendoTrack.toMusicDownloadItem(): com.example.musicplayer.data.MusicDownloadItem {
    val durationMinutes = duration / 60
    val durationSeconds = duration % 60
    val durationFormatted = String.format("%d:%02d", durationMinutes, durationSeconds)

    // 파일 크기 추정 (mp3 128kbps 기준: 약 1MB/분)
    val estimatedSizeMB = (duration / 60.0) * 1.0
    val sizeFormatted = String.format("%.1f MB", estimatedSizeMB)

    // 장르 정보 추출
    val genre = musicInfo?.tags?.genres?.firstOrNull() ?: "Unknown"

    return com.example.musicplayer.data.MusicDownloadItem(
        id = id,
        title = name,
        artist = artistName,
        url = audioDownload ?: audio ?: "",
        duration = durationFormatted,
        size = sizeFormatted,
        genre = genre,
        imageUrl = image ?: albumImage,
        albumName = albumName
    )
}
