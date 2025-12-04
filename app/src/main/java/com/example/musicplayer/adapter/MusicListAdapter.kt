package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentMusicListItemBinding

class MusicListAdapter(
    private val onClick: (MusicFile) -> Unit
) : ListAdapter<MusicFile, MusicListAdapter.MusicViewHolder>(diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = FragmentMusicListItemBinding.inflate(inflater, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MusicViewHolder(private val binding: FragmentMusicListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("DefaultLocale")
        fun bind(music: MusicFile) {
            binding.title.text = music.title

            // 재생 시간 포맷팅
            val durationSeconds = (music.duration / 1000).toInt()
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            binding.duration.text = String.format("%02d:%02d", minutes, seconds)

            binding.root.setOnClickListener { onClick(music) }
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MusicFile>() {
            override fun areItemsTheSame(oldItem: MusicFile, newItem: MusicFile): Boolean = oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: MusicFile, newItem: MusicFile): Boolean = oldItem == newItem
        }
    }
}
