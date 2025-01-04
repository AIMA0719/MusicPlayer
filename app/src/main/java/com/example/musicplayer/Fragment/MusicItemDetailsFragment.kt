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
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.Manager.RecorderManager
import com.example.musicplayer.Manager.ScoreCalculator
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.R
import kotlinx.coroutines.launch
import java.io.File

class MusicItemDetailsFragment : Fragment() {

    companion object {
        fun newInstance(musicItem: MusicItem.MusicItem) = MusicItemDetailsFragment().apply {
            this.musicItem = musicItem
        }
    }

    private lateinit var recorderManager: RecorderManager
    private val viewModel: MusicItemDetailsViewModel by viewModels()
    private var musicItem: MusicItem.MusicItem? = null
    private var isRecording = false
    private var startTime = 0L
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

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
        val view = inflater.inflate(R.layout.fragment_music_item_details, container, false)
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val recordButton: Button = view.findViewById(R.id.recordButton)
        val timerTextView: TextView = view.findViewById(R.id.timerTextView)

        viewModel.musicItem.observe(viewLifecycleOwner) { item ->
            titleTextView.text = item.displayName
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                timerTextView.text = formatTime(elapsedTime)
                timerHandler.postDelayed(this, 1000)
            }
        }

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordButton.text = "Start Recording"
            } else {
                startRecording()
                recordButton.text = "Stop Recording"
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null) // 핸들러 콜백 제거
    }

    private fun startRecording() {
        recorderManager.startRecording()
        isRecording = true
        startTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)
    }

    private fun stopRecording() {
        recorderManager.stopRecording()
        isRecording = false
        timerHandler.removeCallbacks(timerRunnable)
        val recordedFilePath = recorderManager.getRecordedFilePath()

        if (recordedFilePath != null && File(recordedFilePath).exists()) {
            val comparisonManager = ScoreCalculator(requireContext(),50)
            lifecycleScope.launch {
                val score = comparisonManager.compareAudioFiles(Uri.parse(musicItem?.id), recordedFilePath)
                ToastManager.showAnimatedToast(requireContext(), "유사도 점수: $score")
            }
        } else {
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
