package com.example.musicplayer.Adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.musicplayer.ListObjects.MusicItem

object MusicItemDiffCallback : DiffUtil.ItemCallback<MusicItem>() {
    override fun areItemsTheSame(oldItem: MusicItem, newItem: MusicItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MusicItem, newItem: MusicItem): Boolean {
        return oldItem == newItem
    }
}
