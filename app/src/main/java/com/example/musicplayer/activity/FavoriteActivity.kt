package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.FavoriteAdapter
import com.example.musicplayer.manager.ToastManager

class FavoriteActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        setupRecyclerView()
        loadFavorites()
        ToastManager.getInstance().setContext(this)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.favoriteRecyclerView)
        adapter = FavoriteAdapter(
            onItemClick = { song ->
                // 노래 클릭 시 처리
                // TODO: 녹음 화면으로 이동 - RecordingFragment로 대체 필요
                ToastManager.showToast("선택한 노래: ${song.title}")
            },
            onFavoriteClick = { song ->
                // 즐겨찾기 토글 처리
                toggleFavorite(song)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FavoriteActivity)
            adapter = this@FavoriteActivity.adapter
        }
    }

    private fun loadFavorites() {
        // TODO: 데이터베이스에서 즐겨찾기 목록 로드
        // 임시 데이터
        val dummyData = listOf(
            FavoriteSong("노래 1", "가수 1", true, "1"),
            FavoriteSong("노래 2", "가수 2", true, "2"),
            FavoriteSong("노래 3", "가수 3", true, "3")
        )
        adapter.submitList(dummyData)
    }

    private fun toggleFavorite(song: FavoriteSong) {
        // TODO: 데이터베이스에서 즐겨찾기 상태 업데이트
        ToastManager.showToast(
            if (song.isFavorite) "즐겨찾기에서 제거되었습니다" else "즐겨찾기에 추가되었습니다"
        )
    }
}

data class FavoriteSong(
    val title: String,
    val artist: String,
    var isFavorite: Boolean,
    val id: String
) 