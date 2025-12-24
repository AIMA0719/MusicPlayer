package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentScoreResultBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * 녹음 결과 화면 - 레이더 차트와 세부 점수 표시
 */
class ScoreResultFragment : Fragment() {

    private var _binding: FragmentScoreResultBinding? = null
    private val binding get() = _binding!!

    // 점수 데이터
    private var totalScore: Int = 0
    private var pitchAccuracy: Float = 0f
    private var rhythmScore: Float = 0f
    private var vibratoScore: Float = 0f
    private var volumeScore: Float = 0f
    private var longToneScore: Float = 0f
    private var songName: String = ""
    private var songArtist: String = ""
    private var expGained: Int = 0
    private var expRemaining: Int = 0

    companion object {
        private const val ARG_TOTAL_SCORE = "total_score"
        private const val ARG_PITCH_ACCURACY = "pitch_accuracy"
        private const val ARG_RHYTHM_SCORE = "rhythm_score"
        private const val ARG_VIBRATO_SCORE = "vibrato_score"
        private const val ARG_VOLUME_SCORE = "volume_score"
        private const val ARG_LONG_TONE_SCORE = "long_tone_score"
        private const val ARG_SONG_NAME = "song_name"
        private const val ARG_SONG_ARTIST = "song_artist"
        private const val ARG_EXP_GAINED = "exp_gained"
        private const val ARG_EXP_REMAINING = "exp_remaining"

        fun newInstance(
            totalScore: Int,
            pitchAccuracy: Float,
            rhythmScore: Float,
            vibratoScore: Float,
            volumeScore: Float,
            longToneScore: Float,
            songName: String,
            songArtist: String,
            expGained: Int,
            expRemaining: Int
        ): ScoreResultFragment {
            return ScoreResultFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TOTAL_SCORE, totalScore)
                    putFloat(ARG_PITCH_ACCURACY, pitchAccuracy)
                    putFloat(ARG_RHYTHM_SCORE, rhythmScore)
                    putFloat(ARG_VIBRATO_SCORE, vibratoScore)
                    putFloat(ARG_VOLUME_SCORE, volumeScore)
                    putFloat(ARG_LONG_TONE_SCORE, longToneScore)
                    putString(ARG_SONG_NAME, songName)
                    putString(ARG_SONG_ARTIST, songArtist)
                    putInt(ARG_EXP_GAINED, expGained)
                    putInt(ARG_EXP_REMAINING, expRemaining)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            totalScore = it.getInt(ARG_TOTAL_SCORE, 0)
            pitchAccuracy = it.getFloat(ARG_PITCH_ACCURACY, 0f)
            rhythmScore = it.getFloat(ARG_RHYTHM_SCORE, 0f)
            vibratoScore = it.getFloat(ARG_VIBRATO_SCORE, 0f)
            volumeScore = it.getFloat(ARG_VOLUME_SCORE, 0f)
            longToneScore = it.getFloat(ARG_LONG_TONE_SCORE, 0f)
            songName = it.getString(ARG_SONG_NAME, "")
            songArtist = it.getString(ARG_SONG_ARTIST, "")
            expGained = it.getInt(ARG_EXP_GAINED, 0)
            expRemaining = it.getInt(ARG_EXP_REMAINING, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScoreResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScoreDisplay()
        setupRadarChart()
        setupDetailScores()
        setupExpInfo()
        setupButtons()
    }

    @SuppressLint("SetTextI18n")
    private fun setupScoreDisplay() {
        binding.tvTotalScore.text = totalScore.toString()

        // 점수에 따른 색상
        val scoreColor = when {
            totalScore >= 90 -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            totalScore >= 70 -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            totalScore >= 50 -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        }
        binding.tvTotalScore.setTextColor(scoreColor)

        // 등급 텍스트
        val grade = when {
            totalScore >= 95 -> "완벽해요! 🌟"
            totalScore >= 90 -> "훌륭해요! 🎉"
            totalScore >= 80 -> "잘했어요! 👍"
            totalScore >= 70 -> "좋아요! 😊"
            totalScore >= 60 -> "괜찮아요! 💪"
            totalScore >= 50 -> "조금 더 연습해봐요! 📚"
            else -> "다시 도전해봐요! 🎤"
        }
        binding.tvGrade.text = grade

        // 별점 (5점 만점)
        val starCount = when {
            totalScore >= 95 -> 5
            totalScore >= 85 -> 4
            totalScore >= 70 -> 3
            totalScore >= 50 -> 2
            else -> 1
        }
        addStars(starCount)

        // 노래 정보
        val artistText = if (songArtist.isNotEmpty() && songArtist != "<unknown>") songArtist else "알 수 없는 아티스트"
        binding.tvSongInfo.text = "$songName - $artistText"
    }

    private fun addStars(count: Int) {
        binding.llStars.removeAllViews()
        for (i in 1..5) {
            val star = TextView(requireContext()).apply {
                text = if (i <= count) "⭐" else "☆"
                textSize = 24f
            }
            binding.llStars.addView(star)
        }
    }

    private fun setupRadarChart() {
        val chart = binding.radarChart

        // 데이터 엔트리
        val entries = listOf(
            RadarEntry(pitchAccuracy),
            RadarEntry(rhythmScore),
            RadarEntry(vibratoScore),
            RadarEntry(volumeScore),
            RadarEntry(longToneScore)
        )

        val dataSet = RadarDataSet(entries, "점수").apply {
            color = Color.parseColor("#3182F6")
            fillColor = Color.parseColor("#3182F6")
            fillAlpha = 100
            setDrawFilled(true)
            lineWidth = 2f
            valueTextSize = 10f
            valueTextColor = Color.parseColor("#191F28")
        }

        val data = RadarData(dataSet)
        chart.data = data

        // 라벨 설정
        val labels = listOf("음정", "리듬", "비브라토", "볼륨", "롱톤")
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            textSize = 12f
            textColor = Color.parseColor("#191F28")
        }

        // 차트 스타일
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            webLineWidth = 1f
            webColor = Color.parseColor("#E0E0E0")
            webLineWidthInner = 0.5f
            webColorInner = Color.parseColor("#F0F0F0")
            webAlpha = 100

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawLabels(false)
            }

            setTouchEnabled(false)
            animateXY(800, 800)
            invalidate()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDetailScores() {
        binding.tvPitchScore.text = String.format("%.1f%%", pitchAccuracy)
        binding.tvRhythmScore.text = String.format("%.1f%%", rhythmScore)
        binding.tvVibratoScore.text = String.format("%.1f%%", vibratoScore)
        binding.tvVolumeScore.text = String.format("%.1f%%", volumeScore)
        binding.tvLongToneScore.text = String.format("%.1f%%", longToneScore)
    }

    @SuppressLint("SetTextI18n")
    private fun setupExpInfo() {
        if (expGained > 0) {
            binding.cardExp.visibility = View.VISIBLE
            binding.tvExpGained.text = "+$expGained EXP 획득!"
            binding.tvExpRemaining.text = "다음 레벨까지 ${expRemaining} EXP"
        } else {
            binding.cardExp.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnRetry.setOnClickListener {
            // 뒤로 가서 다시 녹음
            findNavController().navigateUp()
        }

        binding.btnDone.setOnClickListener {
            // 노래방 탭으로 이동
            findNavController().popBackStack(R.id.navigation_karaoke, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
