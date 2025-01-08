package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Adapter.MyItemRecyclerViewAdapter
import com.example.musicplayer.MusicPagingSource
import com.example.musicplayer.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_music_list_list, container, false)

        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                val adapter = MyItemRecyclerViewAdapter()
                this.adapter = adapter

                // PagingData 설정
                lifecycleScope.launch {
                    Pager(
                        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                        pagingSourceFactory = { MusicPagingSource(requireContext()) }
                    ).flow.cachedIn(lifecycleScope).collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = MusicListFragment().apply {  }
    }
}

