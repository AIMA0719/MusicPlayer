package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentMusicPlayerBinding
import com.example.musicplayer.manager.ToastManager

/**
 * 음악 재생 Fragment
 * - 깔끔하고 유지보수 쉬운 구조
 * - MediaPlayer를 사용한 음악 재생
 * - 이전곡/다음곡 지원
 */
class MusicPlayerFragment : Fragment() {

    private var _binding: FragmentMusicPlayerBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null
    private var musicList: ArrayList<MusicFile> = arrayListOf()
    private var currentIndex: Int = 0

    // 재생 시간 업데이트를 위한 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var wasPlayingBeforeSwitch = false

    // 재생 시간 업데이트 Runnable
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    val currentPosition = player.currentPosition
                    binding.seekbarProgress.progress = currentPosition
                    binding.tvCurrentTime.text = formatTime(currentPosition)
                }
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // 음악 리스트 받기 (Array를 ArrayList로 변환)
            val musicArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableArray("musicList", MusicFile::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableArray("musicList")
            }

            musicList = if (musicArray != null) {
                ArrayList(musicArray.filterIsInstance<MusicFile>())
            } else {
                arrayListOf()
            }

            // 현재 인덱스 받기
            currentIndex = it.getInt("currentIndex", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (musicList.isEmpty()) {
            ToastManager.showToast("음악 정보를 불러올 수 없습니다")
            findNavController().navigateUp()
            return
        }

        // 현재 인덱스 유효성 검사
        if (currentIndex < 0 || currentIndex >= musicList.size) {
            currentIndex = 0
        }

        setupViews()
        setupMediaPlayer()
        setupSeekBar()
        updateNavigationButtons()
    }

    /**
     * 현재 음악 파일 가져오기
     */
    private fun getCurrentMusic(): MusicFile = musicList[currentIndex]

    /**
     * View 초기 설정
     */
    private fun setupViews() {
        val currentMusic = getCurrentMusic()
        binding.tvSongTitle.text = currentMusic.title
        binding.tvTotalTime.text = formatTime(currentMusic.duration.toInt())
        binding.tvCurrentTime.text = "00:00"

        // 재생/일시정지 버튼
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // 이전 곡 버튼
        binding.btnPrevious.setOnClickListener {
            playPreviousTrack()
        }

        // 다음 곡 버튼
        binding.btnNext.setOnClickListener {
            playNextTrack()
        }
    }

    /**
     * MediaPlayer 설정
     */
    private fun setupMediaPlayer() {
        val currentMusic = getCurrentMusic()
        try {
            // 기존 MediaPlayer 정리
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), currentMusic.uri)
                prepare()

                // SeekBar 최대값 설정
                binding.seekbarProgress.max = duration
                binding.tvTotalTime.text = formatTime(duration)

                // 재생 완료 리스너 - 다음 곡 자동 재생
                setOnCompletionListener {
                    if (currentIndex < musicList.size - 1) {
                        // 다음 곡이 있으면 자동 재생
                        wasPlayingBeforeSwitch = true
                        playNextTrack()
                    } else {
                        // 마지막 곡이면 정지
                        resetPlayPauseButton()
                        binding.seekbarProgress.progress = 0
                        binding.tvCurrentTime.text = "00:00"
                    }
                }
            }

            // 곡 전환 전 재생 중이었으면 자동 재생
            if (wasPlayingBeforeSwitch) {
                mediaPlayer?.start()
                updatePlayPauseButton()
                startProgressUpdate()
                wasPlayingBeforeSwitch = false
            }

        } catch (e: Exception) {
            ToastManager.showToast("음악 파일을 불러올 수 없습니다: ${e.message}")
        }
    }

    /**
     * MediaPlayer 리소스 해제
     */
    private fun releaseMediaPlayer() {
        stopProgressUpdate()
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: IllegalStateException) {
                // MediaPlayer가 잘못된 상태일 수 있음 - 무시하고 release 진행
            } finally {
                try {
                    player.release()
                } catch (e: Exception) {
                    // release 실패도 무시
                }
            }
        }
        mediaPlayer = null
    }

    /**
     * SeekBar 설정
     */
    private fun setupSeekBar() {
        binding.seekbarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                mediaPlayer?.seekTo(seekBar?.progress ?: 0)
            }
        })
    }

    /**
     * 재생/일시정지 토글
     */
    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                resetPlayPauseButton()
            } else {
                player.start()
                updatePlayPauseButton()
                startProgressUpdate()
            }
        }
    }

    /**
     * 재생 버튼으로 변경
     */
    private fun resetPlayPauseButton() {
        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
    }

    /**
     * 일시정지 버튼으로 변경
     */
    private fun updatePlayPauseButton() {
        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
    }

    /**
     * 진행 상태 업데이트 시작
     */
    private fun startProgressUpdate() {
        handler.post(updateProgressRunnable)
    }

    /**
     * 진행 상태 업데이트 중지
     */
    private fun stopProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable)
    }

    /**
     * 이전 곡 재생
     */
    private fun playPreviousTrack() {
        if (currentIndex > 0) {
            // 재생 중이었는지 기억
            wasPlayingBeforeSwitch = mediaPlayer?.isPlaying == true

            // 인덱스 감소
            currentIndex--

            // 곡 전환
            switchTrack()
        }
    }

    /**
     * 다음 곡 재생
     */
    private fun playNextTrack() {
        if (currentIndex < musicList.size - 1) {
            // 재생 중이었는지 기억
            wasPlayingBeforeSwitch = mediaPlayer?.isPlaying == true

            // 인덱스 증가
            currentIndex++

            // 곡 전환
            switchTrack()
        }
    }

    /**
     * 곡 전환 처리
     */
    private fun switchTrack() {
        // UI 업데이트
        val currentMusic = getCurrentMusic()
        binding.tvSongTitle.text = currentMusic.title
        binding.tvCurrentTime.text = "00:00"
        binding.seekbarProgress.progress = 0

        // MediaPlayer 재설정
        setupMediaPlayer()

        // 네비게이션 버튼 상태 업데이트
        updateNavigationButtons()
    }

    /**
     * 네비게이션 버튼 상태 업데이트
     * - 첫 곡: 이전 버튼 비활성화
     * - 마지막 곡: 다음 버튼 비활성화
     */
    private fun updateNavigationButtons() {
        val context = context ?: return

        // 이전 버튼 상태
        if (currentIndex == 0) {
            // 첫 곡 - 비활성화
            binding.btnPrevious.isEnabled = false
            binding.btnPrevious.alpha = 0.3f
            binding.btnPrevious.imageTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
        } else {
            // 활성화
            binding.btnPrevious.isEnabled = true
            binding.btnPrevious.alpha = 1.0f
            binding.btnPrevious.imageTintList = ContextCompat.getColorStateList(context, R.color.primaryColor)
        }

        // 다음 버튼 상태
        if (currentIndex >= musicList.size - 1) {
            // 마지막 곡 - 비활성화
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.3f
            binding.btnNext.imageTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
        } else {
            // 활성화
            binding.btnNext.isEnabled = true
            binding.btnNext.alpha = 1.0f
            binding.btnNext.imageTintList = ContextCompat.getColorStateList(context, R.color.primaryColor)
        }
    }

    /**
     * 시간 포맷팅 (밀리초 -> MM:SS)
     */
    @SuppressLint("DefaultLocale")
    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onPause() {
        super.onPause()
        // 화면을 떠날 때 재생 중지
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                resetPlayPauseButton()
            }
        }
        stopProgressUpdate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseMediaPlayer()
        _binding = null
    }

    companion object {
        fun newInstance(musicList: ArrayList<MusicFile>, currentIndex: Int): MusicPlayerFragment {
            return MusicPlayerFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray("musicList", musicList.toTypedArray())
                    putInt("currentIndex", currentIndex)
                }
            }
        }
    }
}
