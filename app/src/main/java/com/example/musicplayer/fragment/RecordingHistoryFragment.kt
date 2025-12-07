package com.example.musicplayer.fragment

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.RecordingHistoryAdapter
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.extensions.collectInLifecycle
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.viewModel.RecordingHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * 녹음 기록 Fragment
 * - 녹음한 기록을 날짜별로 표시
 * - 재생/삭제 기능
 */
@AndroidEntryPoint
class RecordingHistoryFragment : Fragment() {

    private val viewModel: RecordingHistoryViewModel by viewModels()
    private lateinit var adapter: RecordingHistoryAdapter
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_recording_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        observeViewModel(view)
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvRecordingHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = RecordingHistoryAdapter(
            onItemClick = { recording ->
                showRecordingDetail(recording)
            },
            onPlayClick = { recording ->
                playRecording(recording)
            },
            onDeleteClick = { recording ->
                showDeleteConfirmDialog(recording)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun observeViewModel(view: View) {
        val tvTotalCount = view.findViewById<TextView>(R.id.tvTotalCount)
        val tvAverageScore = view.findViewById<TextView>(R.id.tvAverageScore)
        val tvHighestScore = view.findViewById<TextView>(R.id.tvHighestScore)
        val tvUniqueSongs = view.findViewById<TextView>(R.id.tvUniqueSongs)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmpty)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvRecordingHistory)

        viewModel.uiState.collectInLifecycle(viewLifecycleOwner) { state ->
            // 로딩 상태
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // 빈 상태
            if (state.isEmpty && !state.isLoading) {
                layoutEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            // 통계 업데이트
            tvTotalCount.text = state.stats.totalCount.toString()
            tvAverageScore.text = String.format("%.1f", state.stats.averageScore)
            tvHighestScore.text = state.stats.highestScore.toString()
            tvUniqueSongs.text = state.stats.uniqueSongs.toString()

            // 리스트 업데이트
            adapter.submitList(state.recordings)
        }
    }

    private fun showRecordingDetail(recording: RecordingHistoryEntity) {
        val detailMessage = buildString {
            appendLine("곡: ${recording.songName}")
            appendLine("아티스트: ${recording.songArtist.ifEmpty { "알 수 없음" }}")
            appendLine()
            appendLine("총점: ${recording.totalScore}점")
            appendLine("음정 정확도: ${String.format("%.1f", recording.pitchAccuracy)}%")
            appendLine("리듬 점수: ${String.format("%.1f", recording.rhythmScore)}%")
            appendLine("볼륨 안정성: ${String.format("%.1f", recording.volumeStability)}%")
            appendLine("길이 일치도: ${String.format("%.1f", recording.durationMatch)}%")
            if (recording.hasVibrato) {
                appendLine("비브라토 점수: ${String.format("%.1f", recording.vibratoScore)}%")
            }
            appendLine()
            appendLine("난이도: ${getDifficultyText(recording.difficulty)}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("녹음 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("확인", null)
            .setNeutralButton("재생") { _, _ ->
                playRecording(recording)
            }
            .show()
    }

    private fun playRecording(recording: RecordingHistoryEntity) {
        val file = File(recording.recordingFilePath)
        if (!file.exists()) {
            ToastManager.showToast("녹음 파일을 찾을 수 없습니다")
            return
        }

        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recording.recordingFilePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlayback()
                    ToastManager.showToast("재생 완료")
                }
            }
            ToastManager.showToast("재생 중: ${recording.songName}")
        } catch (e: Exception) {
            ToastManager.showToast("재생 실패: ${e.message}")
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    private fun showDeleteConfirmDialog(recording: RecordingHistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("녹음 기록 삭제")
            .setMessage("'${recording.songName}' 녹음 기록을 삭제하시겠습니까?\n\n녹음 파일도 함께 삭제됩니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteRecording(recording)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteRecording(recording: RecordingHistoryEntity) {
        // 녹음 파일 삭제
        val file = File(recording.recordingFilePath)
        if (file.exists()) {
            file.delete()
        }

        // DB에서 삭제
        viewModel.deleteRecording(recording.id)
        ToastManager.showToast("삭제되었습니다")
    }

    private fun getDifficultyText(difficulty: String): String {
        return when (difficulty) {
            "VERY_EASY" -> "매우 쉬움"
            "EASY" -> "쉬움"
            "NORMAL" -> "보통"
            "HARD" -> "어려움"
            "VERY_HARD" -> "매우 어려움"
            else -> "보통"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayback()
    }

    companion object {
        fun newInstance() = RecordingHistoryFragment()
    }
}
