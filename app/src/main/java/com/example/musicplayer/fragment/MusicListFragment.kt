package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicListAdapter
import com.example.musicplayer.manager.IOSStyleProgressDialog
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.databinding.FragmentMusicListBinding
import com.example.musicplayer.factory.MusicListViewModelFactory
import com.example.musicplayer.viewModel.MusicListViewModel
import kotlinx.coroutines.launch

class MusicListFragment : Fragment() {

    private val viewModel: MusicListViewModel by lazy {
        val application = requireActivity().application
        val factory = MusicListViewModelFactory(application)
        ViewModelProvider(this, factory)[MusicListViewModel::class.java]
    }

    private lateinit var binding: FragmentMusicListBinding
    private lateinit var adapter: MusicListAdapter
    private lateinit var progressDialog: IOSStyleProgressDialog
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMusicListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressDialog = IOSStyleProgressDialog(requireContext())

        adapter = MusicListAdapter { selected ->
            viewModel.onIntent(MusicListIntent.AnalyzeOriginalMusic(selected))
        }

        binding.list.adapter = adapter

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.stateFlow.collect { state ->
                    LogManager.d("MusicListFragment state: isAnalyzing=${state.isAnalyzing}, " +
                            "hasOriginalPitch=${state.originalPitch != null}, " +
                            "hasSelectedMusic=${state.selectedMusic != null}, " +
                            "hasNavigated=${state.hasNavigated}")

                    if (state.isAnalyzing) {
                        progressDialog.show("${state.analysisProgress}% 분석 중...")
                        progressDialog.updateMessage("${state.analysisProgress}% 분석 중...")
                    } else {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss()
                        }
                    }

                    if (state.originalPitch != null && state.selectedMusic != null && !state.isAnalyzing && !state.hasNavigated) {
                        // 페이지 이동
                        LogManager.i("MusicListFragment: Navigating to RecordingFragment")
                        try {
                            val bundle = bundleOf(
                                "music" to state.selectedMusic,
                                "pitchArray" to state.originalPitch.toFloatArray(),
                                "durationMillis" to state.selectedMusic.duration
                            )
                            findNavController().navigate(R.id.action_musicList_to_recording, bundle)
                            // 이동 완료 플래그 설정
                            viewModel.onIntent(MusicListIntent.MarkAsNavigated)
                        } catch (e: Exception) {
                            LogManager.e("Navigation failed: ${e.message}")
                        }
                    }

                    adapter.submitList(state.musicFiles)

                    // 빈 리스트 처리
                    if (state.musicFiles.isEmpty() && !state.isAnalyzing) {
                        binding.tvEmptyList.visibility = View.VISIBLE
                        binding.list.visibility = View.GONE
                    } else {
                        binding.tvEmptyList.visibility = View.GONE
                        binding.list.visibility = View.VISIBLE
                    }
                }
            }
        }

        // 초기 로드
        viewModel.onIntent(MusicListIntent.LoadMusicFiles)
    }

    override fun onResume() {
        super.onResume()
        // 화면으로 돌아올 때마다 리스트 새로고침 (녹음 파일 반영)
        viewModel.onIntent(MusicListIntent.LoadMusicFiles)
        // 네비게이션 상태 초기화 (다시 분석 후 이동 가능하도록)
        viewModel.onIntent(MusicListIntent.MarkAsNavigated)
    }

    override fun onDestroyView() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        if (progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): Fragment = MusicListFragment()
    }
}
