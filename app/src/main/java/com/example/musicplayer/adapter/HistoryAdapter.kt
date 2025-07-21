package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.RecordingHistory

class HistoryAdapter(
    private val onItemClick: (RecordingHistory) -> Unit
) : ListAdapter<RecordingHistory, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (RecordingHistory) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)

        fun bind(recording: RecordingHistory) {
            titleTextView.text = recording.title
            dateTextView.text = recording.date
            scoreTextView.text = "${recording.score}점"

            itemView.setOnClickListener {
                onItemClick(recording)
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<RecordingHistory>() {
        override fun areItemsTheSame(oldItem: RecordingHistory, newItem: RecordingHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecordingHistory, newItem: RecordingHistory): Boolean {
            return oldItem == newItem
        }
    }
} 