package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.activity.SearchSong

class SearchAdapter(
    private val onItemClick: (SearchSong) -> Unit
) : ListAdapter<SearchSong, SearchAdapter.ViewHolder>(SearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)

        fun bind(song: SearchSong) {
            titleTextView.text = song.title
            artistTextView.text = song.artist
            itemView.setOnClickListener { onItemClick(song) }
        }
    }

    private class SearchDiffCallback : DiffUtil.ItemCallback<SearchSong>() {
        override fun areItemsTheSame(oldItem: SearchSong, newItem: SearchSong): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchSong, newItem: SearchSong): Boolean {
            return oldItem == newItem
        }
    }
} 