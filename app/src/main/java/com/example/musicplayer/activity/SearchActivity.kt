package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.SearchAdapter
import com.example.musicplayer.manager.ToastManager
import com.google.android.material.textfield.TextInputEditText

class SearchActivity : AppCompatActivity() {
    private lateinit var searchInput: TextInputEditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setupViews()
        setupRecyclerView()
        ToastManager.getInstance().setContext(this)
    }

    private fun setupViews() {
        searchInput = findViewById(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s.toString())
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.searchRecyclerView)
        adapter = SearchAdapter(
            onItemClick = { song ->
                // 노래 클릭 시 처리
                // TODO: 녹음 화면으로 이동 - RecordingFragment로 대체 필요
                ToastManager.showToast("선택한 노래: ${song.title}")
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    private fun performSearch(query: String) {
        // TODO: 실제 검색 구현
        // 임시 데이터
        val dummyData = if (query.isEmpty()) {
            emptyList()
        } else {
            listOf(
                SearchSong("노래 1", "가수 1", "1"),
                SearchSong("노래 2", "가수 2", "2"),
                SearchSong("노래 3", "가수 3", "3")
            ).filter { it.title.contains(query, ignoreCase = true) }
        }
        adapter.submitList(dummyData)
    }
}

data class SearchSong(
    val title: String,
    val artist: String,
    val id: String
) 