package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.data.MusicDownloadItem
import com.example.musicplayer.databinding.ItemMusicDownloadBinding

class MusicDownloadAdapter(
    private val onDownloadClick: (MusicDownloadItem) -> Unit
) : ListAdapter<MusicDownloadItem, MusicDownloadAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMusicDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MusicDownloadItem) {
            binding.apply {
                tvTitle.text = item.title
                tvArtist.text = item.artist
                tvDuration.text = item.duration
                tvSize.text = item.size

                btnDownload.setOnClickListener {
                    onDownloadClick(item)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MusicDownloadItem>() {
            override fun areItemsTheSame(
                oldItem: MusicDownloadItem,
                newItem: MusicDownloadItem
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: MusicDownloadItem,
                newItem: MusicDownloadItem
            ): Boolean = oldItem == newItem
        }
    }
}