package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Adapter.MyItemRecyclerViewAdapter
import com.example.musicplayer.R
import com.example.musicplayer.ViewModel.MusicViewModel
import com.example.musicplayer.ViewModel.MusicViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicListFragment : Fragment() {

    private val musicViewModel: MusicViewModel by viewModels {
        MusicViewModelFactory(requireContext())
    }

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

                lifecycleScope.launch {
                    musicViewModel.musicFlow.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = MusicListFragment().apply { }
    }
}
