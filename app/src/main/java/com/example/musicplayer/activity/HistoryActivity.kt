package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.HistoryAdapter
import com.example.musicplayer.manager.ToastManager

class HistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.historyRecyclerView)
        adapter = HistoryAdapter { recording ->
            // 녹음 항목 클릭 시 처리
            // TODO: 녹음 상세 화면으로 이동
            ToastManager.showToast("녹음 기록: ${recording.title} (${recording.score}점)")
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
        }
    }

    private fun loadHistory() {
        // TODO: 데이터베이스에서 녹음 기록 로드
        // 임시 데이터
        val dummyData = listOf(
            RecordingHistory(1, "노래 1", "2024-03-20", 85),
            RecordingHistory(2, "노래 2", "2024-03-19", 92),
            RecordingHistory(3, "노래 3", "2024-03-18", 78)
        )
        adapter.submitList(dummyData)
    }
}

data class RecordingHistory(
    val id: Long = 0,
    val title: String,
    val date: String,
    val score: Int
) 