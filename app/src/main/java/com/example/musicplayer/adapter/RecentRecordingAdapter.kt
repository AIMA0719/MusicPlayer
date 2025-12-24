package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.entity.RecordingHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 마이룸용 간단한 최근 녹음 어댑터
 */
class RecentRecordingAdapter(
    private val onItemClick: (RecordingHistoryEntity) -> Unit
) : ListAdapter<RecordingHistoryEntity, RecentRecordingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_recording, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        view: View,
        private val onItemClick: (RecordingHistoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvSongName: TextView = view.findViewById(R.id.tvSongName)
        private val tvArtist: TextView = view.findViewById(R.id.tvArtist)
        private val tvScore: TextView = view.findViewById(R.id.tvScore)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        @SuppressLint("SetTextI18n")
        fun bind(entity: RecordingHistoryEntity) {
            tvSongName.text = entity.songName
            tvArtist.text = entity.songArtist.ifEmpty { "알 수 없음" }
            tvScore.text = "${entity.totalScore}점"
            tvDate.text = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA).format(Date(entity.timestamp))

            // 점수에 따른 색상
            val scoreColor = when {
                entity.totalScore >= 90 -> 0xFF4CAF50.toInt()
                entity.totalScore >= 70 -> 0xFF2196F3.toInt()
                entity.totalScore >= 50 -> 0xFFFF9800.toInt()
                else -> 0xFFF44336.toInt()
            }
            tvScore.setTextColor(scoreColor)

            itemView.setOnClickListener { onItemClick(entity) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecordingHistoryEntity>() {
        override fun areItemsTheSame(
            oldItem: RecordingHistoryEntity,
            newItem: RecordingHistoryEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: RecordingHistoryEntity,
            newItem: RecordingHistoryEntity
        ): Boolean = oldItem == newItem
    }
}
