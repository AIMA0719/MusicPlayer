package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.AchievementEntity
import com.example.musicplayer.manager.AuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        setupRecyclerView(view)
        loadAchievements(view)
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAchievements)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AchievementAdapter()
        recyclerView.adapter = adapter
    }

    private fun loadAchievements(view: View) {
        val tvCount = view.findViewById<TextView>(R.id.tvAchievementCount)
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        lifecycleScope.launch {
            val achievements = database.achievementDao().getAllByUser(userId).first()
            val unlockedCount = achievements.count { it.isUnlocked }
            val totalCount = Achievement.values().size

            tvCount.text = "$unlockedCount / $totalCount"

            // Map entities to display models
            val displayList = Achievement.values().map { achievement ->
                val entity = achievements.find { it.achievementId == achievement.id }
                AchievementDisplayModel(achievement, entity)
            }

            adapter.submitList(displayList)
        }
    }

    data class AchievementDisplayModel(
        val achievement: Achievement,
        val entity: AchievementEntity?
    )

    inner class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

        private var items = listOf<AchievementDisplayModel>()

        fun submitList(newItems: List<AchievementDisplayModel>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
    }

    companion object {
        fun newInstance() = AchievementsFragment()
    }
}
