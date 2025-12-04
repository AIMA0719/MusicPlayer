package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.adapter.MusicListAdapter
import com.example.musicplayer.data.MusicFile
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
            // 클릭 시 다이얼로그 표시
            showMusicActionDialog(selected)
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

    /**
     * 음악 액션 선택 다이얼로그 표시
     */
    @SuppressLint("InflateParams")
    private fun showMusicActionDialog(music: MusicFile) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_music_action, null)
        )
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 음악 제목 설정
        dialog.findViewById<TextView>(R.id.tv_music_title).text = music.title

        // 녹음하기 버튼
        dialog.findViewById<LinearLayout>(R.id.btn_go_recording).setOnClickListener {
            dialog.dismiss()
            // 기존 로직: 음악 분석 후 녹음 페이지로 이동
            viewModel.onIntent(MusicListIntent.AnalyzeOriginalMusic(music))
        }

        // 음악 듣기 버튼
        dialog.findViewById<LinearLayout>(R.id.btn_play_music).setOnClickListener {
            dialog.dismiss()
            // 음악 재생 페이지로 이동
            navigateToMusicPlayer(music)
        }

        // 취소 버튼
        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 너비 설정
        dialog.window?.let { window ->
            val displayMetrics = requireContext().resources.displayMetrics
            val marginDp = 10f
            val marginPx = (marginDp * displayMetrics.density).toInt()
            val params = window.attributes
            params.width = displayMetrics.widthPixels - (marginPx * 2)
            window.attributes = params
        }
    }

    /**
     * 음악 재생 페이지로 이동
     */
    private fun navigateToMusicPlayer(music: MusicFile) {
        // 현재 상태에서 음악 리스트 가져오기
        val currentMusicList = viewModel.container.stateFlow.value.musicFiles

        // 선택한 음악의 인덱스 찾기
        val currentIndex = currentMusicList.indexOf(music).coerceAtLeast(0)

        val bundle = bundleOf(
            "musicList" to currentMusicList.toTypedArray(),
            "currentIndex" to currentIndex
        )
        findNavController().navigate(R.id.action_musicList_to_musicPlayer, bundle)
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
