package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.LevelSystem
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var userRepository: UserRepository
    private lateinit var database: AppDatabase

    companion object {
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
        loadUserProfile(view)
        loadGameData(view)
        loadScoreData(view)
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 데이터 갱신
        view?.let {
            loadGameData(it)
            loadScoreData(it)
        }
    }

    private fun setupViews(view: View) {
        // View All Achievements Button - Navigate directly to achievements
        view.findViewById<View>(R.id.btnViewAllAchievements).setOnClickListener {
            findNavController().navigate(R.id.achievementsFragment)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadUserProfile(view: View) {
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)

        val userId = AuthManager.getCurrentUserId() ?: "guest"
        lifecycleScope.launch {
            val user = userRepository.getUserById(userId)
            if (user != null) {
                tvUserName.text = "${user.displayName}님"
                tvUserEmail.text = user.email ?: "이메일 없음"

                // If guest, show specific message
                if (user.loginType == LoginType.GUEST) {
                    tvUserEmail.text = "게스트 로그인"
                }
            } else {
                tvUserName.text = "사용자 정보 없음"
                tvUserEmail.text = "다시 로그인해주세요"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadGameData(view: View) {
        val tvLevelTitle = view.findViewById<TextView>(R.id.tvLevelTitle)
        val tvLevel = view.findViewById<TextView>(R.id.tvLevel)
        val tvExpProgress = view.findViewById<TextView>(R.id.tvExpProgress)
        val progressExp = view.findViewById<ProgressBar>(R.id.progressExp)
        val tvTotalRecordings = view.findViewById<TextView>(R.id.tvTotalRecordings)
        val tvHighestScore = view.findViewById<TextView>(R.id.tvHighestScore)
        val tvConsecutiveDays = view.findViewById<TextView>(R.id.tvConsecutiveDays)
        val tvAchievementProgress = view.findViewById<TextView>(R.id.tvAchievementProgress)
        val llRecentAchievements = view.findViewById<LinearLayout>(R.id.llRecentAchievements)
        val tvNoAchievements = view.findViewById<TextView>(R.id.tvNoAchievements)

        val userId = AuthManager.getCurrentUserId() ?: "guest"

        lifecycleScope.launch {
            try {
                // Load user level data
                val userLevel = database.userLevelDao().getByUserIdSync(userId)
                com.example.musicplayer.manager.LogManager.i("MainFragment.loadGameData - userId: $userId, userLevel: $userLevel")
                com.example.musicplayer.manager.LogManager.i("MainFragment.loadGameData - totalRecordings: ${userLevel?.totalRecordings}")

                if (userLevel != null) {
                    tvLevelTitle.text = LevelSystem.getLevelTitle(userLevel.level)
                    tvLevel.text = "Lv.${userLevel.level}"

                    val requiredExp = LevelSystem.getRequiredExp(userLevel.level)
                    tvExpProgress.text = "${userLevel.experience} / $requiredExp"
                    progressExp.max = requiredExp
                    progressExp.progress = userLevel.experience

                    tvTotalRecordings.text = userLevel.totalRecordings.toString()
                    tvHighestScore.text = userLevel.highestScore.toString()
                    tvConsecutiveDays.text = "${userLevel.consecutiveDays}일 연속"
                }

                // Load achievements
                val allAchievements = database.achievementDao().getAllByUser(userId)
                allAchievements.collect { achievements ->
                    val unlockedCount = achievements.count { it.isUnlocked }
                    val totalCount = Achievement.entries.size
                    tvAchievementProgress.text = "$unlockedCount / $totalCount"

                    // Show recent unlocked achievements
                    val recentUnlocked = achievements.filter { it.isUnlocked }
                        .sortedByDescending { it.unlockedAt }
                        .take(3)

                    llRecentAchievements.removeAllViews()
                    if (recentUnlocked.isEmpty()) {
                        llRecentAchievements.addView(tvNoAchievements)
                    } else {
                        recentUnlocked.forEach { entity ->
                            val achievement = Achievement.entries.find { it.id == entity.achievementId }
                            if (achievement != null) {
                                val itemView = createAchievementItemView(achievement)
                                llRecentAchievements.addView(itemView)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createAchievementItemView(achievement: Achievement): View {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            setPadding(0, 12, 0, 12)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Icon
        val iconText = TextView(requireContext()).apply {
            text = achievement.icon
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 16, 0)
            }
        }

        // Info container
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Title
        val titleText = TextView(requireContext()).apply {
            text = achievement.title
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        // Description
        val descText = TextView(requireContext()).apply {
            text = achievement.description
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        infoLayout.addView(titleText)
        infoLayout.addView(descText)

        // Unlocked badge
        val badgeText = TextView(requireContext()).apply {
            text = "✓"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        itemLayout.addView(iconText)
        itemLayout.addView(infoLayout)
        itemLayout.addView(badgeText)

        return itemLayout
    }

    @SuppressLint("SetTextI18n")
    private fun loadScoreData(view: View) {
        val tvMonthlyAverage = view.findViewById<TextView>(R.id.tvMonthlyAverage)
        val tvMonthlyAverageSubtext = view.findViewById<TextView>(R.id.tvMonthlyAverageSubtext)
        val llTop3Container = view.findViewById<LinearLayout>(R.id.llTop3Container)
        val tvNoScores = view.findViewById<TextView>(R.id.tvNoScores)

        val userId = AuthManager.getCurrentUserId()
        if (userId != null) {
            lifecycleScope.launch {
                try {
                    // 현재 월의 시작과 끝 타임스탬프 계산
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.timeInMillis

                    calendar.add(Calendar.MONTH, 1)
                    val endOfMonth = calendar.timeInMillis

                    // 월 평균 점수 조회
                    val averageScore = database.scoreDao().getMonthlyAverageScore(userId, startOfMonth, endOfMonth)
                    if (averageScore != null && averageScore > 0) {
                        tvMonthlyAverage.text = "${averageScore.toInt()}점"
                        tvMonthlyAverageSubtext.text = "이번 달 평균 점수입니다"
                    } else {
                        tvMonthlyAverage.text = "0점"
                        tvMonthlyAverageSubtext.text = "노래를 녹음하고 점수를 받아보세요!"
                    }

                    // Top 3 점수 조회
                    val top3Scores = database.scoreDao().getMonthlyTop3Scores(userId, startOfMonth, endOfMonth)

                    // Top 3 컨테이너 초기화
                    llTop3Container.removeAllViews()

                    if (top3Scores.isEmpty()) {
                        // 기록이 없을 때
                        llTop3Container.addView(tvNoScores)
                    } else {
                        // Top 3 아이템 동적으로 추가
                        top3Scores.forEachIndexed { index, score ->
                            val itemView = createTop3ItemView(index + 1, score)
                            llTop3Container.addView(itemView)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createTop3ItemView(rank: Int, score: ScoreEntity): View {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            setPadding(0, 12, 0, 12)
        }

        // 순위 표시
        val rankBadge = TextView(requireContext()).apply {
            text = when(rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$rank"
            }
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 16, 0)
            }
        }

        // 곡 정보 컨테이너
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // 곡명
        val songNameText = TextView(requireContext()).apply {
            text = score.songName
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        infoLayout.addView(songNameText)

        // 아티스트명 - <unknown>이거나 비어있으면 표시하지 않음
        val artistName = score.songArtist
        if (artistName.isNotEmpty() && artistName != "<unknown>" && artistName.lowercase() != "unknown") {
            val artistText = TextView(requireContext()).apply {
                text = artistName
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            infoLayout.addView(artistText)
        }

        // 점수 표시
        val scoreText = TextView(requireContext()).apply {
            text = "${score.score}점"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        itemLayout.addView(rankBadge)
        itemLayout.addView(infoLayout)
        itemLayout.addView(scoreText)

        return itemLayout
    }
}
