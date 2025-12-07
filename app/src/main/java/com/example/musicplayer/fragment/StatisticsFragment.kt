package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.musicplayer.R
import com.example.musicplayer.extensions.collectInLifecycle
import com.example.musicplayer.viewModel.DailyRecordingCount
import com.example.musicplayer.viewModel.DifficultyStatItem
import com.example.musicplayer.viewModel.ScoreDistribution
import com.example.musicplayer.viewModel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 통계 Fragment
 * - 녹음 통계를 시각적으로 표시
 */
@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel(view)
    }

    private fun observeViewModel(view: View) {
        val tvTotalRecordings = view.findViewById<TextView>(R.id.tvTotalRecordings)
        val tvAverageScore = view.findViewById<TextView>(R.id.tvAverageScore)
        val tvHighestScore = view.findViewById<TextView>(R.id.tvHighestScore)
        val tvUniqueSongs = view.findViewById<TextView>(R.id.tvUniqueSongs)

        val barExcellent = view.findViewById<View>(R.id.barExcellent)
        val barGood = view.findViewById<View>(R.id.barGood)
        val barAverage = view.findViewById<View>(R.id.barAverage)
        val barNeedsWork = view.findViewById<View>(R.id.barNeedsWork)
        val tvExcellentCount = view.findViewById<TextView>(R.id.tvExcellentCount)
        val tvGoodCount = view.findViewById<TextView>(R.id.tvGoodCount)
        val tvAverageCount = view.findViewById<TextView>(R.id.tvAverageCount)
        val tvNeedsWorkCount = view.findViewById<TextView>(R.id.tvNeedsWorkCount)

        val llWeeklyChart = view.findViewById<LinearLayout>(R.id.llWeeklyChart)
        val llDifficultyStats = view.findViewById<LinearLayout>(R.id.llDifficultyStats)
        val tvNoDifficultyData = view.findViewById<TextView>(R.id.tvNoDifficultyData)

        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmpty)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        viewModel.uiState.collectInLifecycle(viewLifecycleOwner) { state ->
            // 로딩 상태
            progressBar.isVisible = state.isLoading

            // 빈 상태
            layoutEmpty.isVisible = state.isEmpty && !state.isLoading

            // 기본 통계
            tvTotalRecordings.text = state.totalRecordings.toString()
            tvAverageScore.text = String.format("%.1f", state.averageScore)
            tvHighestScore.text = state.highestScore.toString()
            tvUniqueSongs.text = state.uniqueSongs.toString()

            // 점수 분포
            updateScoreDistribution(
                state.scoreDistribution,
                barExcellent, barGood, barAverage, barNeedsWork,
                tvExcellentCount, tvGoodCount, tvAverageCount, tvNeedsWorkCount
            )

            // 주간 트렌드
            updateWeeklyChart(llWeeklyChart, state.weeklyTrend)

            // 난이도별 통계
            updateDifficultyStats(llDifficultyStats, tvNoDifficultyData, state.difficultyStats)
        }
    }

    private fun updateScoreDistribution(
        distribution: ScoreDistribution,
        barExcellent: View, barGood: View, barAverage: View, barNeedsWork: View,
        tvExcellent: TextView, tvGood: TextView, tvAverage: TextView, tvNeedsWork: TextView
    ) {
        val maxCount = maxOf(
            distribution.excellent,
            distribution.good,
            distribution.average,
            distribution.needsWork,
            1 // 최소 1로 나눠서 0으로 나누기 방지
        )

        // 바 너비 설정 (최대 값 기준 비율)
        barExcellent.post {
            val parentWidth = (barExcellent.parent as View).width
            barExcellent.layoutParams.width = (parentWidth * distribution.excellent / maxCount)
            barExcellent.requestLayout()
        }
        barGood.post {
            val parentWidth = (barGood.parent as View).width
            barGood.layoutParams.width = (parentWidth * distribution.good / maxCount)
            barGood.requestLayout()
        }
        barAverage.post {
            val parentWidth = (barAverage.parent as View).width
            barAverage.layoutParams.width = (parentWidth * distribution.average / maxCount)
            barAverage.requestLayout()
        }
        barNeedsWork.post {
            val parentWidth = (barNeedsWork.parent as View).width
            barNeedsWork.layoutParams.width = (parentWidth * distribution.needsWork / maxCount)
            barNeedsWork.requestLayout()
        }

        // 카운트 텍스트
        tvExcellent.text = "${distribution.excellent}회"
        tvGood.text = "${distribution.good}회"
        tvAverage.text = "${distribution.average}회"
        tvNeedsWork.text = "${distribution.needsWork}회"
    }

    private fun updateWeeklyChart(container: LinearLayout, weeklyTrend: List<DailyRecordingCount>) {
        container.removeAllViews()

        if (weeklyTrend.isEmpty()) return

        val maxCount = weeklyTrend.maxOfOrNull { it.count } ?: 1
        val maxBarHeight = 80 // dp
        val density = resources.displayMetrics.density

        for (item in weeklyTrend) {
            val dayLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
            }

            // 카운트 텍스트
            val countText = TextView(requireContext()).apply {
                text = if (item.count > 0) item.count.toString() else ""
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(0xFF666666.toInt())
            }

            // 바
            val barHeight = if (maxCount > 0) {
                ((item.count.toFloat() / maxCount) * maxBarHeight * density).toInt()
            } else {
                0
            }
            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (24 * density).toInt(),
                    maxOf(barHeight, (4 * density).toInt()) // 최소 높이
                ).apply {
                    topMargin = (4 * density).toInt()
                    bottomMargin = (4 * density).toInt()
                }
                setBackgroundResource(R.drawable.bg_bar_chart)
            }

            // 요일 텍스트
            val dayText = TextView(requireContext()).apply {
                text = item.dayName
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(0xFF8B95A1.toInt())
            }

            dayLayout.addView(countText)
            dayLayout.addView(bar)
            dayLayout.addView(dayText)

            container.addView(dayLayout)
        }
    }

    private fun updateDifficultyStats(
        container: LinearLayout,
        emptyText: TextView,
        stats: List<DifficultyStatItem>
    ) {
        container.removeAllViews()

        if (stats.isEmpty()) {
            emptyText.isVisible = true
            return
        }

        emptyText.isVisible = false

        for (item in stats) {
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * resources.displayMetrics.density).toInt()
                }
            }

            // 난이도 이름
            val nameText = TextView(requireContext()).apply {
                text = item.difficultyName
                textSize = 14f
                setTextColor(0xFF191F28.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // 횟수
            val countText = TextView(requireContext()).apply {
                text = "${item.count}회"
                textSize = 13f
                setTextColor(0xFF8B95A1.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (16 * resources.displayMetrics.density).toInt()
                }
            }

            // 평균 점수
            val scoreText = TextView(requireContext()).apply {
                text = "평균 ${String.format("%.1f", item.averageScore)}점"
                textSize = 14f
                setTextColor(getScoreColor(item.averageScore))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            itemLayout.addView(nameText)
            itemLayout.addView(countText)
            itemLayout.addView(scoreText)

            container.addView(itemLayout)
        }
    }

    private fun getScoreColor(score: Double): Int {
        return when {
            score >= 90 -> 0xFF4CAF50.toInt() // 녹색
            score >= 70 -> 0xFF2196F3.toInt() // 파랑
            score >= 50 -> 0xFFFF9800.toInt() // 주황
            else -> 0xFFF44336.toInt() // 빨강
        }
    }

    companion object {
        fun newInstance() = StatisticsFragment()
    }
}
