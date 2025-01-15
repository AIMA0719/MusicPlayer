package com.example.musicplayer.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Fragment.MusicItemDetailsFragment
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.Manager.FragmentMoveManager
import com.example.musicplayer.Manager.LogManager
import com.example.musicplayer.databinding.FragmentMusicListBinding

class MyItemRecyclerViewAdapter : PagingDataAdapter<MusicItem, MyItemRecyclerViewAdapter.ViewHolder>(MusicItemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentMusicListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ViewHolder(binding: FragmentMusicListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.fileName
        val contentView: TextView = binding.fileUri

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { item ->
                        FragmentMoveManager.instance.pushFragment(MusicItemDetailsFragment.newInstance(item))
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: MusicItem) {
            idView.text = "파일명 : " + item.fileName
            contentView.text = "파일 uri : $item"
        }
    }
}

