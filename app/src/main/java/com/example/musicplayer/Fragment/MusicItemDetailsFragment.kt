package com.example.musicplayer.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.Manager.ProgressDialogManager
import com.example.musicplayer.Manager.RecorderManager
import com.example.musicplayer.Manager.ScoreCalculator
import com.example.musicplayer.Manager.ScoreDialogManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.databinding.FragmentMusicItemDetailsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class MusicItemDetailsFragment : Fragment() {

    companion object {
        fun newInstance(musicItem: MusicItem) = MusicItemDetailsFragment().apply {
            this.musicItem = musicItem
        }
    }

    private val job = Job()
    private lateinit var recorderManager: RecorderManager
    private val viewModel: MusicItemDetailsViewModel by viewModels()
    private var musicItem: MusicItem? = null
    private var isRecording = false
    private var startTime = 0L
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private lateinit var binding: FragmentMusicItemDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicItem?.let { viewModel.setMusicItem(musicItem!!) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        recorderManager = RecorderManager(context)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMusicItemDetailsBinding.inflate(inflater, container, false)

        viewModel.musicItem.observe(viewLifecycleOwner) { item ->
            binding.titleTextView.text = item.toString()
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                binding.timerTextView.text = formatTime(elapsedTime)
                timerHandler.postDelayed(this, 1000)
            }
        }

        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                binding.recordButton.text = "Start Recording"
            } else {
                startRecording()
                binding.recordButton.text = "Stop Recording"
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // 모든 비동기 작업 취소
        timerHandler.removeCallbacksAndMessages(null)
        ProgressDialogManager.dismiss()
        ScoreDialogManager.dismiss()
    }


    private fun startRecording() {
        recorderManager.startRecording()
        isRecording = true
        startTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)
    }

    @SuppressLint("SetTextI18n")
    private fun stopRecording() {
        recorderManager.stopRecording()
        isRecording = false
        timerHandler.removeCallbacks(timerRunnable)
        val recordedFilePath = recorderManager.getRecordedFilePath()

        if (recordedFilePath != null && File(recordedFilePath).exists()) {
            ProgressDialogManager.show(requireContext())
            val comparisonManager = ScoreCalculator(requireContext(), 50)
            lifecycleScope.launch {
                binding.timerTextView.visibility = View.GONE
                binding.timerTextView.text = ""
                val score = comparisonManager.compareAudioFiles(Uri.parse(musicItem?.id), recordedFilePath)
                ProgressDialogManager.dismiss() // Progress Dialog 닫기
                ScoreDialogManager.show(requireContext(), score) // Score Dialog 표시
            }
        } else {
            binding.timerTextView.visibility = View.GONE
            binding.timerTextView.text = ""
            ToastManager.showAnimatedToast(requireContext(), "녹음 파일이 유효하지 않습니다.")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
