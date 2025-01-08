package com.example.musicplayer.Adapter

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
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

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

        fun bind(item: MusicItem) {
            idView.text = item.id
            LogManager.d("item.id : " + item.id)
            contentView.text = item.toString()
        }
    }
}

