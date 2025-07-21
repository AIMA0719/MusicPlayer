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
import com.example.musicplayer.activity.FavoriteSong

class FavoriteAdapter(
    private val onItemClick: (FavoriteSong) -> Unit,
    private val onFavoriteClick: (FavoriteSong) -> Unit
) : ListAdapter<FavoriteSong, FavoriteAdapter.ViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favoriteButton)

        fun bind(song: FavoriteSong) {
            titleTextView.text = song.title
            artistTextView.text = song.artist
            favoriteButton.setImageResource(
                if (song.isFavorite) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )

            itemView.setOnClickListener { onItemClick(song) }
            favoriteButton.setOnClickListener { onFavoriteClick(song) }
        }
    }

    private class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteSong>() {
        override fun areItemsTheSame(oldItem: FavoriteSong, newItem: FavoriteSong): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoriteSong, newItem: FavoriteSong): Boolean {
            return oldItem == newItem
        }
    }
} 