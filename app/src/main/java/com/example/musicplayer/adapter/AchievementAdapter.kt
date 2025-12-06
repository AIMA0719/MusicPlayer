package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.viewModel.AchievementDisplayModel

/**
 * 도전과제 Adapter (ListAdapter + DiffUtil)
 */
class AchievementAdapter : ListAdapter<AchievementDisplayModel, AchievementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        private val llProgress: LinearLayout = view.findViewById(R.id.llProgress)
        private val tvProgress: TextView = view.findViewById(R.id.tvProgress)
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        private val tvUnlockedBadge: TextView = view.findViewById(R.id.tvUnlockedBadge)
        private val tvLockedIcon: TextView = view.findViewById(R.id.tvLockedIcon)

        fun bind(model: AchievementDisplayModel) {
            val achievement = model.achievement
            val entity = model.entity

            tvIcon.text = achievement.icon
            tvTitle.text = achievement.title
            tvDescription.text = achievement.description

            val isUnlocked = entity?.isUnlocked == true

            if (isUnlocked) {
                // Unlocked
                tvIcon.alpha = 1.0f
                tvUnlockedBadge.visibility = View.VISIBLE
                tvLockedIcon.visibility = View.GONE
                llProgress.visibility = View.GONE
            } else {
                // Locked
                tvIcon.alpha = 0.3f
                tvUnlockedBadge.visibility = View.GONE
                tvLockedIcon.visibility = View.VISIBLE

                // Show progress if available
                if (entity != null && entity.maxProgress > 1) {
                    llProgress.visibility = View.VISIBLE
                    tvProgress.text = "${entity.progress} / ${entity.maxProgress}"
                    progressBar.max = entity.maxProgress
                    progressBar.progress = entity.progress
                } else {
                    llProgress.visibility = View.GONE
                }
            }
        }
    }

    /**
     * DiffUtil Callback
     */
    private class DiffCallback : DiffUtil.ItemCallback<AchievementDisplayModel>() {
        override fun areItemsTheSame(
            oldItem: AchievementDisplayModel,
            newItem: AchievementDisplayModel
        ): Boolean {
            return oldItem.achievement.id == newItem.achievement.id
        }

        override fun areContentsTheSame(
            oldItem: AchievementDisplayModel,
            newItem: AchievementDisplayModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
