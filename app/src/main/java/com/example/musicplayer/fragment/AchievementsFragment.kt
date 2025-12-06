package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.AchievementAdapter
import com.example.musicplayer.extensions.collectInLifecycle
import com.example.musicplayer.viewModel.AchievementsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 도전과제 Fragment
 * - ViewModel 사용
 * - ListAdapter + DiffUtil 적용
 */
@AndroidEntryPoint
class AchievementsFragment : Fragment() {

    private val viewModel: AchievementsViewModel by viewModels()
    private lateinit var adapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        observeViewModel(view)
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAchievements)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AchievementAdapter()
        recyclerView.adapter = adapter
    }

    private fun observeViewModel(view: View) {
        val tvCount = view.findViewById<TextView>(R.id.tvAchievementCount)

        viewModel.uiState.collectInLifecycle(viewLifecycleOwner) { state ->
            tvCount.text = "${state.unlockedCount} / ${state.totalCount}"
            adapter.submitList(state.achievements)
        }
    }

    companion object {
        fun newInstance() = AchievementsFragment()
    }
}
