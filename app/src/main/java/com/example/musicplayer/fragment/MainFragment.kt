package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var userRepository: UserRepository
    private lateinit var database: AppDatabase

    companion object {
        private const val DAILY_GOAL = 3

        @JvmStatic
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        setupViews(view)
        loadData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadData(it) }
    }

    private fun setupViews(view: View) {
        view.findViewById<View>(R.id.btnViewAllAchievements).setOnClickListener {
            findNavController().navigate(R.id.achievementsFragment)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadData(view: View) {
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        lifecycleScope.launch {
            try {
                // 인사말 설정
                loadGreeting(view)

                // 사용자 정보
                loadUserInfo(view, userId)

                // 오늘의 데이터
                loadTodayData(view, userId)

                // 주간 트렌드
                loadWeeklyTrend(view, userId)

                // 빠른 시작 (최근 곡)
                loadQuickStart(view, userId)

                // 달성 임박 도전과제
                loadNearAchievements(view, userId)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadGreeting(view: View) {
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        tvGreeting.text = when {
            hour < 6 -> "늦은 밤이에요 🌙"
            hour < 12 -> "좋은 아침이에요! ☀️"
            hour < 18 -> "좋은 오후에요! 🎵"
            else -> "좋은 저녁이에요! 🌆"
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadUserInfo(view: View, userId: String) {
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)

        val user = userRepository.getUserById(userId)
        tvUserName.text = if (user != null) {
            "${user.displayName}님, 오늘도 노래해볼까요?"
        } else {
            "노래 연습을 시작해보세요!"
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadTodayData(view: View, userId: String) {
        val tvDailyProgress = view.findViewById<TextView>(R.id.tvDailyProgress)
        val tvDailyGoalText = view.findViewById<TextView>(R.id.tvDailyGoalText)
        val progressDailyGoal = view.findViewById<ProgressBar>(R.id.progressDailyGoal)
        val tvDailyGoalReward = view.findViewById<TextView>(R.id.tvDailyGoalReward)
        val tvTodayRecordings = view.findViewById<TextView>(R.id.tvTodayRecordings)
        val tvTodayBestScore = view.findViewById<TextView>(R.id.tvTodayBestScore)
        val tvStreakDays = view.findViewById<TextView>(R.id.tvStreakDays)

        // 오늘 시작/끝 타임스탬프
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        // 오늘 녹음 수
        val todayRecordings = database.recordingHistoryDao()
            .getRecordingCountByDateRange(userId, startOfDay, endOfDay)

        tvTodayRecordings.text = todayRecordings.toString()

        // 일일 목표
        val progress = minOf(todayRecordings, DAILY_GOAL)
        tvDailyProgress.text = "$progress / $DAILY_GOAL"
        progressDailyGoal.max = DAILY_GOAL
        progressDailyGoal.progress = progress

        if (todayRecordings >= DAILY_GOAL) {
            tvDailyGoalText.text = "오늘 목표를 달성했어요! 🎉"
            tvDailyGoalReward.text = "✅ +30 EXP 획득 완료!"
            tvDailyGoalReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_success))
        } else {
            val remaining = DAILY_GOAL - todayRecordings
            tvDailyGoalText.text = "오늘 ${remaining}곡 더 녹음하면 목표 달성!"
            tvDailyGoalReward.text = "🎁 달성 시 +30 EXP"
            tvDailyGoalReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_warning))
        }

        // 오늘 최고 점수
        val todayBestScore = database.scoreDao().getBestScoreByDateRange(userId, startOfDay, endOfDay)
        tvTodayBestScore.text = if (todayBestScore != null && todayBestScore > 0) {
            todayBestScore.toString()
        } else {
            "-"
        }

        // 연속일
        val userLevel = database.userLevelDao().getByUserIdSync(userId)
        tvStreakDays.text = (userLevel?.consecutiveDays ?: 0).toString()
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadWeeklyTrend(view: View, userId: String) {
        val tvWeeklyRecordings = view.findViewById<TextView>(R.id.tvWeeklyRecordings)
        val tvWeeklyRecordingsChange = view.findViewById<TextView>(R.id.tvWeeklyRecordingsChange)
        val tvWeeklyAvgScore = view.findViewById<TextView>(R.id.tvWeeklyAvgScore)
        val tvWeeklyScoreChange = view.findViewById<TextView>(R.id.tvWeeklyScoreChange)

        val calendar = Calendar.getInstance()

        // 이번 주 시작 (월요일)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val thisWeekStart = calendar.timeInMillis

        // 이번 주 끝
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val thisWeekEnd = calendar.timeInMillis

        // 지난 주 시작
        calendar.add(Calendar.WEEK_OF_YEAR, -2)
        val lastWeekStart = calendar.timeInMillis

        // 지난 주 끝
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val lastWeekEnd = calendar.timeInMillis

        // 이번 주 녹음 수
        val thisWeekRecordings = database.recordingHistoryDao()
            .getRecordingCountByDateRange(userId, thisWeekStart, thisWeekEnd)
        tvWeeklyRecordings.text = "${thisWeekRecordings}회"

        // 지난 주 녹음 수
        val lastWeekRecordings = database.recordingHistoryDao()
            .getRecordingCountByDateRange(userId, lastWeekStart, lastWeekEnd)

        val recordingDiff = thisWeekRecordings - lastWeekRecordings
        tvWeeklyRecordingsChange.text = when {
            recordingDiff > 0 -> "지난주 대비 +$recordingDiff"
            recordingDiff < 0 -> "지난주 대비 $recordingDiff"
            else -> "지난주와 동일"
        }
        tvWeeklyRecordingsChange.setTextColor(
            ContextCompat.getColor(requireContext(),
                if (recordingDiff >= 0) R.color.glass_success else R.color.glass_error)
        )

        // 이번 주 평균 점수
        val thisWeekAvg = database.scoreDao().getAverageScoreByDateRange(userId, thisWeekStart, thisWeekEnd)
        tvWeeklyAvgScore.text = if (thisWeekAvg != null && thisWeekAvg > 0) {
            "${thisWeekAvg.toInt()}점"
        } else {
            "0점"
        }

        // 지난 주 평균 점수
        val lastWeekAvg = database.scoreDao().getAverageScoreByDateRange(userId, lastWeekStart, lastWeekEnd)

        val scoreDiff = ((thisWeekAvg ?: 0.0) - (lastWeekAvg ?: 0.0)).toInt()
        tvWeeklyScoreChange.text = when {
            scoreDiff > 0 -> "지난주 대비 +$scoreDiff"
            scoreDiff < 0 -> "지난주 대비 $scoreDiff"
            else -> "지난주와 동일"
        }
        tvWeeklyScoreChange.setTextColor(
            ContextCompat.getColor(requireContext(),
                if (scoreDiff >= 0) R.color.glass_success else R.color.glass_error)
        )
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadQuickStart(view: View, userId: String) {
        val layoutRecentSong = view.findViewById<LinearLayout>(R.id.layoutRecentSong)
        val layoutNoRecentSong = view.findViewById<LinearLayout>(R.id.layoutNoRecentSong)
        val tvRecentSongTitle = view.findViewById<TextView>(R.id.tvRecentSongTitle)
        val tvRecentSongArtist = view.findViewById<TextView>(R.id.tvRecentSongArtist)

        val recentRecording = database.recordingHistoryDao().getRecentRecordings(userId, 1).firstOrNull()

        if (recentRecording != null) {
            layoutRecentSong.visibility = View.VISIBLE
            layoutNoRecentSong.visibility = View.GONE

            tvRecentSongTitle.text = recentRecording.songName
            tvRecentSongArtist.text = if (recentRecording.songArtist.isNotEmpty() &&
                recentRecording.songArtist != "<unknown>") {
                recentRecording.songArtist
            } else {
                "알 수 없는 아티스트"
            }
        } else {
            layoutRecentSong.visibility = View.GONE
            layoutNoRecentSong.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadNearAchievements(view: View, userId: String) {
        val layoutNearAchievements = view.findViewById<LinearLayout>(R.id.layoutNearAchievements)
        val tvNoNearAchievements = view.findViewById<TextView>(R.id.tvNoNearAchievements)

        layoutNearAchievements.removeAllViews()

        val achievements = database.achievementDao().getAllByUser(userId).first()

        // 진행 중이면서 50% 이상 진행된 도전과제 (최대 2개)
        val nearAchievements = achievements
            .filter { !it.isUnlocked && it.progress > 0 }
            .map { entity ->
                val achievement = Achievement.entries.find { it.id == entity.achievementId }
                val progress = if (entity.maxProgress > 0) {
                    (entity.progress.toFloat() / entity.maxProgress * 100).toInt()
                } else 0
                Triple(achievement, entity, progress)
            }
            .filter { it.third >= 30 } // 30% 이상 진행된 것만
            .sortedByDescending { it.third }
            .take(2)

        if (nearAchievements.isEmpty()) {
            tvNoNearAchievements.visibility = View.VISIBLE
            layoutNearAchievements.visibility = View.GONE
        } else {
            tvNoNearAchievements.visibility = View.GONE
            layoutNearAchievements.visibility = View.VISIBLE

            nearAchievements.forEach { (achievement, entity, progress) ->
                if (achievement != null) {
                    val itemView = createNearAchievementItem(achievement, entity.progress, entity.maxProgress, progress)
                    layoutNearAchievements.addView(itemView)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createNearAchievementItem(
        achievement: Achievement,
        currentProgress: Int,
        maxProgress: Int,
        percentage: Int
    ): View {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_item)
            setPadding(16, 16, 16, 16)
        }

        // 상단 (아이콘 + 제목 + 진행률)
        val topLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val iconText = TextView(requireContext()).apply {
            text = achievement.icon
            textSize = 20f
        }

        val titleText = TextView(requireContext()).apply {
            text = achievement.title
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
        }

        val progressText = TextView(requireContext()).apply {
            text = "$currentProgress / $maxProgress"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_primary))
        }

        topLayout.addView(iconText)
        topLayout.addView(titleText)
        topLayout.addView(progressText)

        // 프로그레스바
        val progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8
            ).apply {
                topMargin = 8
            }
            max = 100
            progress = percentage
            progressDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_progress_bar)
        }

        itemLayout.addView(topLayout)
        itemLayout.addView(progressBar)

        return itemLayout
    }
}
