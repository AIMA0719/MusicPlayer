package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.musicplayer.adapter.RecentRecordingAdapter
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.databinding.FragmentMyroomBinding
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.LevelSystem
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 마이룸 탭 - 개인 기록실 (프로필, 누적 통계, 녹음 기록, 도전과제)
 */
@AndroidEntryPoint
class MyRoomFragment : Fragment() {

    private var _binding: FragmentMyroomBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var database: AppDatabase
    private lateinit var recordingAdapter: RecentRecordingAdapter

    companion object {
        fun newInstance() = MyRoomFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyroomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        setupViews()
        setupRecyclerView()
        loadAllData()
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }

    private fun setupViews() {
        // 설정 버튼
        binding.btnSettings.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_settings
        }

        // 녹음 기록 전체보기
        binding.btnViewAllRecordings.setOnClickListener {
            findNavController().navigate(R.id.recordingHistoryFragment)
        }

        // 도전과제 전체보기
        binding.btnViewAllAchievements.setOnClickListener {
            findNavController().navigate(R.id.achievementsFragment)
        }

        // 통계 상세보기
        binding.btnViewStatistics.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }
    }

    private fun setupRecyclerView() {
        recordingAdapter = RecentRecordingAdapter { _ ->
            // 녹음 기록 클릭 시
        }

        binding.rvRecentRecordings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordingAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadAllData() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        lifecycleScope.launch {
            loadUserProfile(userId)
            loadCumulativeStats(userId)
            loadRecentRecordings(userId)
            loadMonthlyReport(userId)
        }

        // 도전과제는 Flow로 관찰
        observeAchievements(userId)
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadUserProfile(userId: String) {
        try {
            // 사용자 정보
            val user = userRepository.getUserById(userId)
            if (user != null) {
                binding.tvUserName.text = "${user.displayName}님"

                // 가입일 포맷
                val joinDate = SimpleDateFormat("yyyy.MM", Locale.KOREA).format(Date(user.createdAt))
                binding.tvJoinDate.text = joinDate
            }

            // 레벨 정보
            val userLevel = database.userLevelDao().getByUserIdSync(userId)
            if (userLevel != null) {
                binding.tvLevelBadge.text = "Lv.${userLevel.level}"
                binding.tvLevelTitle.text = LevelSystem.getLevelTitle(userLevel.level)

                val requiredExp = LevelSystem.getRequiredExp(userLevel.level)
                binding.tvExpProgress.text = "${userLevel.experience} / $requiredExp"
                binding.progressExp.max = requiredExp
                binding.progressExp.progress = userLevel.experience

                // 활동일 계산 (녹음 기록이 있는 날짜 수)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, -1) // 최근 1년
                val oneYearAgo = calendar.timeInMillis
                val activeDays = database.recordingHistoryDao().getRecordingDaysInPeriod(userId, oneYearAgo)
                binding.tvTotalDays.text = activeDays.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadCumulativeStats(userId: String) {
        try {
            // 총 녹음 수
            val totalRecordings = database.recordingHistoryDao().getTotalRecordingCount(userId)
            binding.tvTotalRecordings.text = totalRecordings.toString()

            // 전체 평균 점수
            val avgScore = database.recordingHistoryDao().getAverageScore(userId)
            binding.tvAverageScore.text = if (avgScore > 0) avgScore.toInt().toString() else "0"

            // 최고 점수
            val highestScore = database.scoreDao().getHighestScore(userId)
            binding.tvHighestScore.text = (highestScore ?: 0).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadRecentRecordings(userId: String) {
        try {
            val recordings = database.recordingHistoryDao().getRecentRecordings(userId, 5)

            if (recordings.isEmpty()) {
                binding.rvRecentRecordings.visibility = View.GONE
                binding.tvNoRecordings.visibility = View.VISIBLE
            } else {
                binding.rvRecentRecordings.visibility = View.VISIBLE
                binding.tvNoRecordings.visibility = View.GONE
                recordingAdapter.submitList(recordings)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.rvRecentRecordings.visibility = View.GONE
            binding.tvNoRecordings.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun loadMonthlyReport(userId: String) {
        try {
            // 현재 월
            val calendar = Calendar.getInstance()
            val monthName = SimpleDateFormat("M월", Locale.KOREA).format(calendar.time)
            binding.tvMonthlyReportTitle.text = "${monthName} 리포트"

            // 이번 달 시작/끝
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis

            // 이번 달 녹음 수
            val monthlyRecordings = database.recordingHistoryDao()
                .getRecordingCountByDateRange(userId, startOfMonth, endOfMonth)
            binding.tvMonthlyRecordings.text = "${monthlyRecordings}회"

            // 이번 달 평균 점수
            val monthlyAvg = database.scoreDao().getMonthlyAverageScore(userId, startOfMonth, endOfMonth)
            binding.tvMonthlyAverage.text = if (monthlyAvg != null && monthlyAvg > 0) {
                "${monthlyAvg.toInt()}점"
            } else {
                "0점"
            }

            // 이번 달 최고 점수
            val monthlyBest = database.scoreDao().getBestScoreByDateRange(userId, startOfMonth, endOfMonth)
            binding.tvMonthlyBest.text = if (monthlyBest != null && monthlyBest > 0) {
                "${monthlyBest}점"
            } else {
                "0점"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeAchievements(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    database.achievementDao().getAllByUser(userId).collect { achievements ->
                        val unlockedCount = achievements.count { it.isUnlocked }
                        val totalCount = Achievement.entries.size

                        binding.tvAchievementProgress.text = "$unlockedCount / $totalCount"
                        binding.progressAchievements.max = totalCount
                        binding.progressAchievements.progress = unlockedCount

                        // 최근 달성 도전과제 3개
                        val recentUnlocked = achievements
                            .filter { it.isUnlocked }
                            .sortedByDescending { it.unlockedAt }
                            .take(3)

                        binding.layoutRecentAchievements.removeAllViews()

                        if (recentUnlocked.isEmpty()) {
                            binding.tvNoAchievements.visibility = View.VISIBLE
                            binding.layoutRecentAchievements.visibility = View.GONE
                        } else {
                            binding.tvNoAchievements.visibility = View.GONE
                            binding.layoutRecentAchievements.visibility = View.VISIBLE

                            recentUnlocked.forEach { entity ->
                                val achievement = Achievement.entries.find { it.id == entity.achievementId }
                                if (achievement != null) {
                                    val itemView = createAchievementItem(achievement)
                                    binding.layoutRecentAchievements.addView(itemView)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createAchievementItem(achievement: Achievement): View {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_item)
            setPadding(12, 12, 12, 12)
        }

        // 아이콘
        val iconText = TextView(requireContext()).apply {
            text = achievement.icon
            textSize = 20f
        }

        // 제목
        val titleText = TextView(requireContext()).apply {
            text = achievement.title
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
        }

        // 달성 뱃지
        val badgeText = TextView(requireContext()).apply {
            text = "✓"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.glass_success))
        }

        itemLayout.addView(iconText)
        itemLayout.addView(titleText)
        itemLayout.addView(badgeText)

        return itemLayout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
