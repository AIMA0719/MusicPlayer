package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.viewModel.RecordingHistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 녹음 기록 Adapter (ListAdapter + DiffUtil)
 * - 날짜 헤더와 녹음 기록 아이템을 함께 표시
 */
class RecordingHistoryAdapter(
    private val onItemClick: (RecordingHistoryEntity) -> Unit,
    private val onPlayClick: (RecordingHistoryEntity) -> Unit,
    private val onDeleteClick: (RecordingHistoryEntity) -> Unit
) : ListAdapter<RecordingHistoryItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_RECORDING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RecordingHistoryItem.DateHeader -> VIEW_TYPE_HEADER
            is RecordingHistoryItem.Recording -> VIEW_TYPE_RECORDING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_recording_history_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_recording_history, parent, false)
                RecordingViewHolder(view, onItemClick, onPlayClick, onDeleteClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RecordingHistoryItem.DateHeader -> (holder as HeaderViewHolder).bind(item)
            is RecordingHistoryItem.Recording -> (holder as RecordingViewHolder).bind(item.entity)
        }
    }

    /**
     * 날짜 헤더 ViewHolder
     */
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDateHeader)

        fun bind(item: RecordingHistoryItem.DateHeader) {
            tvDate.text = item.date
        }
    }

    /**
     * 녹음 기록 ViewHolder
     */
    class RecordingViewHolder(
        view: View,
        private val onItemClick: (RecordingHistoryEntity) -> Unit,
        private val onPlayClick: (RecordingHistoryEntity) -> Unit,
        private val onDeleteClick: (RecordingHistoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvSongName: TextView = view.findViewById(R.id.tvSongName)
        private val tvArtist: TextView = view.findViewById(R.id.tvArtist)
        private val tvScore: TextView = view.findViewById(R.id.tvScore)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
        private val tvPitchAccuracy: TextView = view.findViewById(R.id.tvPitchAccuracy)
        private val tvRhythmScore: TextView = view.findViewById(R.id.tvRhythmScore)
        private val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(entity: RecordingHistoryEntity) {
            tvSongName.text = entity.songName
            tvArtist.text = entity.songArtist.ifEmpty { "알 수 없음" }
            tvScore.text = "${entity.totalScore}점"
            tvTime.text = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(entity.timestamp))

            // 난이도 표시
            val difficultyText = when (entity.difficulty) {
                "VERY_EASY" -> "매우 쉬움"
                "EASY" -> "쉬움"
                "NORMAL" -> "보통"
                "HARD" -> "어려움"
                "VERY_HARD" -> "매우 어려움"
                else -> "보통"
            }
            tvDifficulty.text = difficultyText

            // 세부 점수
            tvPitchAccuracy.text = "음정 ${String.format("%.1f", entity.pitchAccuracy)}%"
            tvRhythmScore.text = "리듬 ${String.format("%.1f", entity.rhythmScore)}%"

            // 점수에 따른 색상
            val scoreColor = when {
                entity.totalScore >= 90 -> 0xFF4CAF50.toInt() // 녹색
                entity.totalScore >= 70 -> 0xFF2196F3.toInt() // 파랑
                entity.totalScore >= 50 -> 0xFFFF9800.toInt() // 주황
                else -> 0xFFF44336.toInt() // 빨강
            }
            tvScore.setTextColor(scoreColor)

            // 클릭 리스너
            itemView.setOnClickListener { onItemClick(entity) }
            btnPlay.setOnClickListener { onPlayClick(entity) }
            btnDelete.setOnClickListener { onDeleteClick(entity) }
        }
    }

    /**
     * DiffUtil Callback
     */
    private class DiffCallback : DiffUtil.ItemCallback<RecordingHistoryItem>() {
        override fun areItemsTheSame(
            oldItem: RecordingHistoryItem,
            newItem: RecordingHistoryItem
        ): Boolean {
            return when {
                oldItem is RecordingHistoryItem.DateHeader && newItem is RecordingHistoryItem.DateHeader ->
                    oldItem.date == newItem.date
                oldItem is RecordingHistoryItem.Recording && newItem is RecordingHistoryItem.Recording ->
                    oldItem.entity.id == newItem.entity.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: RecordingHistoryItem,
            newItem: RecordingHistoryItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
