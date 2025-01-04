package com.example.musicplayer.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Fragment.MusicItemDetailsFragment
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.Manager.FragmentMoveManager
import com.example.musicplayer.Manager.MusicLoaderManager
import com.example.musicplayer.databinding.FragmentMusicListBinding

class MyItemRecyclerViewAdapter(context: Context) :
    RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {
    private val musicList: List<MusicItem.MusicItem> = MusicLoaderManager.loadMusic(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentMusicListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = musicList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = musicList.size

    inner class ViewHolder(binding: FragmentMusicListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = musicList[position]
                    FragmentMoveManager.instance.pushFragment(MusicItemDetailsFragment.newInstance(item))
                }
            }
        }

        fun bind(item: MusicItem.MusicItem) {
            idView.text = item.id
            contentView.text = item.displayName
        }

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}
