package com.example.musicplayer.Adapter

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Manager.MusicLoaderManager
import com.example.musicplayer.Manager.UriUtils
import com.example.musicplayer.databinding.FragmentMusicListBinding
import com.example.musicplayer.placeholder.PlaceholderContent
import java.io.IOException

class MyItemRecyclerViewAdapter(context: Context) :
    RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private val musicList: List<PlaceholderContent.PlaceholderItem> = MusicLoaderManager.loadMusic(context)
    private var mediaPlayer: MediaPlayer? = null

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
                    playMusic(item)
                }
            }
        }

        fun bind(item: PlaceholderContent.PlaceholderItem) {
            idView.text = item.id
            contentView.text = item.displayName
        }

        private fun playMusic(item: PlaceholderContent.PlaceholderItem) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            } else {
                mediaPlayer?.reset()
            }

            try {
                val filePath = UriUtils.getMediaFilePath(itemView.context, item.id)
                filePath?.let {
                    mediaPlayer?.setDataSource(it)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mediaPlayer?.setOnCompletionListener {
                stopMusic()
            }
        }

        private fun stopMusic() {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        }

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}
