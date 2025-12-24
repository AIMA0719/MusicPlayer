package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.RecentRecordingAdapter
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.databinding.FragmentMyroomBinding
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.LevelSystem
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 마이룸 탭 - 프로필, 통계, 녹음 기록, 도전과제
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
        loadUserProfile()
        loadStatistics()
        loadRecentRecordings()
        loadAchievements()
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 데이터 갱신
        loadStatistics()
        loadRecentRecordings()
        loadAchievements()
    }

    private fun setupViews() {
        // 설정 버튼
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
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
        binding.cardStatistics.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }
    }

    private fun setupRecyclerView() {
        recordingAdapter = RecentRecordingAdapter { recording ->
            // 녹음 기록 클릭 시 (추후 상세 보기 구현)
        }

        binding.rvRecentRecordings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordingAdapter
            isNestedScrollingEnabled = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadUserProfile() {
        val userId = AuthManager.getCurrentUserId() ?: "guest"

        lifecycleScope.launch {
            try {
                // 사용자 정보
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    binding.tvUserName.text = "${user.displayName}님"
                }

                // 레벨 정보
                val userLevel = database.userLevelDao().getByUserIdSync(userId)
                if (userLevel != null) {
                    binding.tvLevelTitle.text = LevelSystem.getLevelTitle(userLevel.level)
                    binding.tvLevel.text = "Lv.${userLevel.level}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadStatistics() {
        val userId = AuthManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                // 총 녹음 수
                val userLevel = database.userLevelDao().getByUserIdSync(userId)
                binding.tvTotalRecordings.text = (userLevel?.totalRecordings ?: 0).toString()

                // 평균 점수 (이번 달)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                val endOfMonth = calendar.timeInMillis

                val averageScore = database.scoreDao().getMonthlyAverageScore(userId, startOfMonth, endOfMonth)
                binding.tvAverageScore.text = if (averageScore != null && averageScore > 0) {
                    "${averageScore.toInt()}점"
                } else {
                    "0점"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentRecordings() {
        val userId = AuthManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                val recordings = database.recordingHistoryDao()
                    .getRecentRecordings(userId, 5)

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
    }

    @SuppressLint("SetTextI18n")
    private fun loadAchievements() {
        val userId = AuthManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                database.achievementDao().getAllByUser(userId).collect { achievements ->
                    val unlockedCount = achievements.count { it.isUnlocked }
                    val totalCount = Achievement.entries.size

                    binding.tvAchievementProgress.text = "$unlockedCount / $totalCount"
                    binding.progressAchievements.max = totalCount
                    binding.progressAchievements.progress = unlockedCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
