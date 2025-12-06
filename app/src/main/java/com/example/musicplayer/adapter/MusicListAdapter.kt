package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentMusicListItemBinding
import java.io.File

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

            // 아티스트 표시 - <unknown>이 아닌 경우에만 표시
            val artistName = music.artist
            if (artistName.isNotEmpty() && artistName != "<unknown>" && artistName.lowercase() != "unknown") {
                binding.artist.text = artistName
                binding.artist.visibility = View.VISIBLE
            } else {
                binding.artist.visibility = View.GONE
            }

            // 재생 시간 포맷팅
            val durationSeconds = (music.duration / 1000).toInt()
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            binding.duration.text = String.format("%02d:%02d", minutes, seconds)

            // 파일 크기 표시
            val fileSize = getFileSize(music)
            binding.fileSize.text = formatFileSize(fileSize)

            binding.root.setOnClickListener { onClick(music) }
        }

        private fun getFileSize(music: MusicFile): Long {
            return try {
                val context = binding.root.context
                // file:// URI인 경우 직접 파일 접근
                if (music.uri.scheme == "file") {
                    val file = File(music.uri.path ?: return 0L)
                    file.length()
                } else {
                    // content:// URI인 경우 ContentResolver 사용
                    val cursor = context.contentResolver.query(
                        music.uri,
                        arrayOf(android.provider.OpenableColumns.SIZE),
                        null, null, null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                        } else 0L
                    } ?: 0L
                }
            } catch (_: Exception) {
                0L
            }
        }

        @SuppressLint("DefaultLocale")
        private fun formatFileSize(size: Long): String {
            return when {
                size <= 0 -> "-"
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MusicFile>() {
            override fun areItemsTheSame(oldItem: MusicFile, newItem: MusicFile): Boolean = oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: MusicFile, newItem: MusicFile): Boolean = oldItem == newItem
        }
    }
}
