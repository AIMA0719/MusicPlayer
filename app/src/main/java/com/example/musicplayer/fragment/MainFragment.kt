package com.example.musicplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.R
import com.example.musicplayer.activity.LoginActivity
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.entity.ScoreEntity
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.*

class MainFragment : Fragment() {

    private lateinit var userRepository: UserRepository
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

        userRepository = UserRepository(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        setupViews(view)
        loadUserProfile(view)
        loadScoreData(view)
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 점수 데이터 갱신
        view?.let { loadScoreData(it) }
    }

    private fun setupViews(view: View) {
        // Logout Button
        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun loadUserProfile(view: View) {
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)

        val userId = AuthManager.getCurrentUserId()
        if (userId != null) {
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
        } else {
            tvUserName.text = "로그인 필요"
            tvUserEmail.text = "로그인해주세요"
        }
    }

    private fun showLogoutConfirmDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

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

        // 아티스트명
        val artistText = TextView(requireContext()).apply {
            text = if (score.songArtist.isNotEmpty()) score.songArtist else "알 수 없음"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        infoLayout.addView(songNameText)
        infoLayout.addView(artistText)

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

    private fun performLogout() {
        // Google 로그아웃
        val user = lifecycleScope.launch {
            val userId = AuthManager.getCurrentUserId()
            if (userId != null) {
                val userEntity = userRepository.getUserById(userId)
                if (userEntity != null && userEntity.loginType == LoginType.GOOGLE) {
                    com.example.musicplayer.manager.GoogleAuthManager.signOut {
                        // Google 로그아웃 완료 후 처리
                    }
                }
            }
        }

        AuthManager.logout()
        ContextManager.clearContext()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
