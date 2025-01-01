package com.example.musicplayer.ListObjects

import java.io.File

// MusicItem 관리 객체로, 음악 데이터를 저장하고 관리
object MusicItem {

    // MusicItem 객체를 저장하는 리스트
    private val ITEMS: MutableList<MusicItem> = ArrayList()

    // MusicItem 객체를 ID로 접근할 수 있는 맵
    private val ITEM_MAP: MutableMap<String, MusicItem> = HashMap()

    // 샘플 데이터를 생성하기 위한 개수 상수
    private const val COUNT = 25

    // 초기화 블록: 객체가 생성될 때 샘플 데이터를 추가
    init {
        // COUNT만큼의 샘플 MusicItem 추가
        for (i in 1..COUNT) {
            addItem(createPlaceholderItem(i))
        }
    }

    // MusicItem을 리스트와 맵에 추가
    private fun addItem(item: MusicItem) {
        ITEMS.add(item) // 리스트에 추가
        ITEM_MAP[item.id] = item // 맵에 추가
    }

    // 위치를 기반으로 샘플 MusicItem 생성
    private fun createPlaceholderItem(position: Int): MusicItem {
        return MusicItem(
            position.toString(), // ID
            "Item $position", // 내용
            makeDetails(position) // 세부 내용
        )
    }

    // 샘플 MusicItem의 세부 내용을 생성
    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position) // 기본 세부 내용 추가
        for (i in 0 until position) { // 위치 값만큼 반복하여 추가 내용 생성
            builder.append("\nMore details information here.") // 추가 세부 내용
        }
        return builder.toString()
    }

    // MusicItem 데이터 클래스 정의
    data class MusicItem(val id: String, val content: String, val details: String) {
        // 파일 이름을 반환하는 프로퍼티
        val displayName: String
            get() = File(content).name

        // MusicItem의 문자열 표현을 파일 이름으로 반환
        override fun toString(): String = displayName
    }

}
