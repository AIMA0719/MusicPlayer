package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicplayer.adapter.MusicListAdapter
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.databinding.FragmentMusicListBinding
import com.example.musicplayer.factory.MusicListViewModelFactory
import com.example.musicplayer.viewmodel.MusicListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicListFragment : Fragment() {

    private val viewModel: MusicListViewModel by lazy {
        val application = requireActivity().application
        val factory = MusicListViewModelFactory(application)
        ViewModelProvider(this, factory)[MusicListViewModel::class.java]
    }

    private lateinit var binding: FragmentMusicListBinding
    private lateinit var adapter: MusicListAdapter
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMusicListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MusicListAdapter { selected ->
            viewModel.onIntent(MusicListIntent.AnalyzeOriginalMusic(selected))
        }

        binding.list.adapter = adapter

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    if (state.isAnalyzing) {
                        binding.loadingLayout.visibility = View.VISIBLE
                        binding.loadingProgressBar.progress = state.analysisProgress
                        binding.loadingText.text = "${state.analysisProgress}% 분석 중"
                    } else {
                        binding.loadingLayout.visibility = View.GONE
                    }

                    if (state.originalPitch != null && state.selectedMusic != null && !state.isAnalyzing) {
                        FragmentMoveManager.instance.pushFragment(
                            RecordingFragment.newInstance(
                                music = state.selectedMusic,
                                originalPitch = state.originalPitch.toFloatArray(),
                                durationMillis = state.selectedMusic.duration
                            )
                        )
                    }

                    adapter.submitList(state.musicFiles)
                }
            }
        }

        viewModel.onIntent(MusicListIntent.LoadMusicFiles)
    }

    override fun onDestroyView() {
        mediaPlayer?.release()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): Fragment = MusicListFragment()
    }
}
