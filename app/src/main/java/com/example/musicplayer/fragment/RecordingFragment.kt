package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentRecordingBinding
import com.example.musicplayer.manager.ScoreFeedbackDialogManager
import com.example.musicplayer.viewModel.ScoreViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordingFragment : Fragment() {

    private val scoreViewModel: ScoreViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val viewModel: RecordingViewModel by viewModels()

    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray
    private var durationMillis: Long = 0

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private var wasRecording = false
    private var lastUserX = -1f
    private var lastOriginIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("music", MusicFile::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("music")!!
            }
            pitchArray = it.getFloatArray("pitchArray")!!
            durationMillis = it.getLong("durationMillis", 0L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        initPitchChart()
        setupViews()
        setObserver()

        return binding.root
    }

    private fun setupViews() {
        // 노래 제목 설정
        binding.songTitle.text = music.title

        // 시작 버튼
        binding.btnStart.setOnClickListener {
            viewModel.startRecording(pitchArray)
        }

        // 일시정지/재개 버튼
        binding.btnPause.setOnClickListener {
            if (viewModel.container.stateFlow.value.isPaused) {
                viewModel.resumeRecording()
            } else {
                viewModel.pauseRecording()
            }
        }

        // 정지 버튼
        binding.btnStop.setOnClickListener {
            viewModel.stopRecording()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setObserver() {
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        // State Flow collection
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.stateFlow.collect { state ->
                    // Update elapsed time
                    val elapsedMs = state.elapsedTime
                    val clampedElapsed = elapsedMs.coerceAtMost(durationMillis)
                    val elapsed = formatMillisToTime(clampedElapsed)
                    val total = formatMillisToTime(durationMillis)
                    binding.timeDisplay.text = "$elapsed / $total"

                    // 원본 피치를 시간에 맞춰 그리기 (녹음 중일 때 자동으로)
                    val index = (clampedElapsed / 100).toInt()
                    if (index > lastOriginIndex && state.isRecording && !state.isPaused) {
                        for (i in lastOriginIndex + 1..index) {
                            val pitch = pitchArray.getOrNull(i) ?: 0f
                            addOriginPitchEntry(pitch, i / 10f)
                        }
                        lastOriginIndex = index

                        // 차트 업데이트 (사용자가 말하지 않아도 자동으로 스크롤)
                        val chart = binding.pitchChart
                        val data = chart.data
                        if (data != null) {
                            data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.setVisibleXRangeMaximum(10f)
                            val currentTimeSec = clampedElapsed / 1000f
                            chart.moveViewToX((currentTimeSec - 5f).coerceAtLeast(0f))
                            chart.invalidate()
                        }
                    }

                    if (clampedElapsed >= durationMillis && state.isRecording && !state.isPaused) {
                        viewModel.stopRecording()
                    }

                    // Update current pitch
                    val pitch = state.currentPitch
                    val diff = state.pitchDifference
                    if (state.isRecording && !state.isPaused) {
                        binding.pitchDifference.text = "%.1f Hz (오차: %.1f Hz)".format(pitch, diff)
                    } else {
                        binding.pitchDifference.text = "대기 중..."
                    }

                    if (pitch > 0 && !state.isPaused) {
                        addUserPitchEntry(pitch, elapsedMs / 1000f)
                    }

                    // Update recording/pause state UI
                    updateRecordingUI(state.isRecording, state.isPaused)

                    // Update realtime score
                    if (state.isRecording) {
                        binding.scoreCard.visibility = View.VISIBLE
                        binding.currentScore.text = state.currentScore.toString()
                        binding.accuracyText.text = "%.1f%%".format(state.accuracy)
                    } else {
                        binding.scoreCard.visibility = View.GONE
                    }

                    // Update final score
                    state.score?.let { baseScore ->
                        // ScoreAnalyzer 가져오기
                        val analyzer = viewModel.getScoreAnalyzer()
                        if (analyzer != null) {
                            // 1단계: 채점 난이도 선택 다이얼로그 표시
                            ScoreFeedbackDialogManager.showDifficultySelectDialog(
                                requireContext(),
                                baseScore
                            ) { adjustedScore, difficulty ->
                                // 점수 저장
                                scoreViewModel.saveScore(music.title, adjustedScore, music.artist)

                                // 2단계: 상세 피드백 다이얼로그 표시
                                ScoreFeedbackDialogManager.showScoreFeedbackDialog(
                                    requireContext(),
                                    analyzer,
                                    adjustedScore,
                                    difficulty
                                )
                            }
                        } else {
                            // Fallback: analyzer가 없으면 토스트로 표시
                            Toast.makeText(requireContext(), "점수 계산 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Side effect collection
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.sideEffectFlow.collect { sideEffect ->
                    when (sideEffect) {
                        is RecordingSideEffect.ClearChart -> {
                            // 차트 초기화
                            lastUserX = -1f
                            lastOriginIndex = -1
                            initPitchChart()
                        }
                        is RecordingSideEffect.ShowError -> {
                            Toast.makeText(requireContext(), sideEffect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateRecordingUI(isRecording: Boolean, isPaused: Boolean) {
        if (isRecording) {
            if (isPaused) {
                // 일시정지 상태
                binding.recordingStatus.text = "일시정지"
                binding.btnStart.visibility = View.GONE
                binding.btnPause.visibility = View.VISIBLE
                binding.btnStop.visibility = View.VISIBLE
                binding.btnPause.text = "▶"
                binding.btnPause.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_green_light
                )
            } else {
                // 녹음 중
                binding.recordingStatus.text = "녹음 중..."
                binding.btnStart.visibility = View.GONE
                binding.btnPause.visibility = View.VISIBLE
                binding.btnStop.visibility = View.VISIBLE
                binding.btnPause.text = "⏸"
                binding.btnPause.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_orange_dark
                )
            }
            wasRecording = true
        } else {
            // 녹음 대기 중
            binding.recordingStatus.text = "녹음 대기 중"
            binding.btnStart.visibility = View.VISIBLE
            binding.btnPause.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            wasRecording = false
        }
    }

    private fun addUserPitchEntry(userPitch: Float, xSec: Float) {
        val chart = binding.pitchChart
        val data = chart.data ?: return
        val userDataSet = data.getDataSetByIndex(0)

        if (xSec <= lastUserX) return  // 역순 방지
        lastUserX = xSec

        userDataSet.addEntry(Entry(xSec, userPitch))
        adjustYAxisIfNeeded(userPitch)
        trimAndRefreshChart(data)
    }

    private fun addOriginPitchEntry(originalPitch: Float, xSec: Float) {
        val chart = binding.pitchChart
        val data = chart.data ?: return
        val originDataSet = data.getDataSetByIndex(1)

        originDataSet.addEntry(Entry(xSec, originalPitch))
        adjustYAxisIfNeeded(originalPitch)
    }

    private fun adjustYAxisIfNeeded(newPitch: Float) {
        val chart = binding.pitchChart
        val axis = chart.axisLeft
        if (newPitch > axis.axisMaximum) {
            axis.axisMaximum = (newPitch * 1.1f).coerceAtLeast(100f)
            chart.invalidate() // Y축 변경 즉시 반영
        }
    }

    private fun trimAndRefreshChart(data: LineData) {
        val chart = binding.pitchChart
        val userDataSet = data.getDataSetByLabel("내 음정", true) ?: return

        // 사용자 피치만 트림 (원본은 전체 유지)
        if (userDataSet.entryCount > 100 && userDataSet.entryCount > 1) {
            userDataSet.removeFirst()
        }

        if (userDataSet.entryCount < 1) return

        data.notifyDataChanged()
        chart.notifyDataSetChanged()

        // 화면에 보이는 범위를 10초로 설정
        chart.setVisibleXRangeMaximum(10f)

        // 현재 시간을 따라가도록 차트 이동
        if (userDataSet.entryCount > 0) {
            val lastX = userDataSet.getEntryForIndex(userDataSet.entryCount - 1).x
            chart.moveViewToX((lastX - 5f).coerceAtLeast(0f)) // 중앙에 현재 시간 배치
        }

        chart.invalidate()
    }

    private fun initPitchChart() {
        val chart = binding.pitchChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(true)
        chart.setGridBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        chart.setDrawBorders(true)
        chart.setBorderColor(android.graphics.Color.parseColor("#E0E0E0"))
        chart.setBorderWidth(1f)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textSize = 12f
        chart.legend.textColor = android.graphics.Color.parseColor("#666666")

        // Y축 설정 - 초기값을 작게 시작 (동적으로 증가)
        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 300f // 초기 최대값을 300으로 시작
        leftAxis.textColor = android.graphics.Color.parseColor("#666666")
        leftAxis.gridColor = android.graphics.Color.parseColor("#E0E0E0")
        leftAxis.setDrawGridLines(true)

        // X축 설정
        val xAxis = chart.xAxis
        xAxis.isEnabled = true
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = android.graphics.Color.parseColor("#E0E0E0")
        xAxis.textColor = android.graphics.Color.parseColor("#666666")
        xAxis.granularity = 1f
        xAxis.labelCount = 5
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "%.0f초".format(value)
        }

        // 사용자 피치 데이터셋 (진한 파란색, 실시간 추가)
        val userDataSet = LineDataSet(mutableListOf(), "내 음정").apply {
            color = android.graphics.Color.parseColor("#2196F3")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 3f
            setDrawFilled(false)
            mode = LineDataSet.Mode.LINEAR
        }

        // 원본 피치 데이터셋 (시간에 맞춰 추가될 예정, 빈 상태로 시작)
        val originalDataSet = LineDataSet(mutableListOf(), "원곡 멜로디").apply {
            color = android.graphics.Color.parseColor("#FF9800")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = android.graphics.Color.parseColor("#FFECB3")
            fillAlpha = 50
            mode = LineDataSet.Mode.LINEAR
        }

        chart.data = LineData(userDataSet, originalDataSet)
        chart.invalidate()
    }

    @SuppressLint("DefaultLocale")
    private fun formatMillisToTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopRecording()
        _binding = null
    }

    companion object {
        fun newInstance(music: MusicFile, originalPitch: FloatArray, durationMillis: Long): RecordingFragment {
            return RecordingFragment().apply {
                arguments = bundleOf(
                    "music" to music,
                    "pitchArray" to originalPitch,
                    "durationMillis" to durationMillis
                )
            }
        }
    }
}
