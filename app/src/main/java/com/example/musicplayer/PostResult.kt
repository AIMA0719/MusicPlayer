package com.example.musicplayer

import com.google.gson.annotations.SerializedName

class PostResult {
    @SerializedName("userId")
    lateinit var userId: String

    @SerializedName("id")
    lateinit var id: String

    @SerializedName("title") // "title"로 수정
    lateinit var title: String

    @SerializedName("body")
    lateinit var bodyValue: String

    override fun toString(): String {
        return "PostResult(userId='$userId', id='$id', title='$title', bodyValue='$bodyValue')"
    }
}

