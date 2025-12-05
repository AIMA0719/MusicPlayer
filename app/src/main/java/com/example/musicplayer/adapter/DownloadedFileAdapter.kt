package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.databinding.ItemDownloadedFileBinding
import com.example.musicplayer.manager.FileDownloadManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadedFileAdapter(
    private val onItemClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : ListAdapter<File, DownloadedFileAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadedFileBinding.inflate(
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
        private val binding: ItemDownloadedFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            binding.apply {
                tvFilename.text = file.nameWithoutExtension.replace("_", " ")
                tvFilePath.text = file.parent?.substringAfterLast("/") ?: "Downloads"
                tvFileSize.text = FileDownloadManager.formatFileSize(file.length())

                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                tvDateDownloaded.text = dateFormat.format(Date(file.lastModified()))

                // 전체 아이템 클릭 시 다이얼로그 표시
                root.setOnClickListener {
                    onItemClick(file)
                }

                // 재생 버튼도 동일하게 다이얼로그 표시
                btnPlay.setOnClickListener {
                    onItemClick(file)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(file)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(oldItem: File, newItem: File): Boolean =
                oldItem.absolutePath == newItem.absolutePath

            override fun areContentsTheSame(oldItem: File, newItem: File): Boolean =
                oldItem.lastModified() == newItem.lastModified() && oldItem.length() == newItem.length()
        }
    }
}