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
        setObserver()

        binding.micImage.setOnClickListener {
            if (viewModel.container.stateFlow.value.isRecording) {
                viewModel.stopRecording()
            } else {
                viewModel.startRecording(pitchArray)
            }
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun setObserver() {
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        //LogManager.e(listOf(pitchArray.toList()))

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

                    val index = (clampedElapsed / 100).toInt()
                    if (index > lastOriginIndex) {
                        for (i in lastOriginIndex + 1..index) {
                            val pitch = pitchArray.getOrNull(i) ?: 0f
                            addOriginPitchEntry(pitch, i / 10f)
                        }
                        lastOriginIndex = index
                    }

                    if (clampedElapsed >= durationMillis && state.isRecording) {
                        viewModel.stopRecording()
                    }

                    // Update current pitch
                    val pitch = state.currentPitch
                    val diff = state.pitchDifference
                    binding.pitchDifference.text = "\uD83C\uDFB5 현재 pitch: %.2f Hz / 오차: %.2f Hz".format(pitch, diff)
                    if (pitch > 0) {
                        addUserPitchEntry(pitch, elapsedMs / 1000f)
                    }

                    // Update recording state
                    binding.micImage.alpha = if (state.isRecording) 1.0f else 0.5f

                    // Update score
                    state.score?.let { score ->
                        Toast.makeText(requireContext(), "점수: $score 점", Toast.LENGTH_LONG).show()
                        scoreViewModel.saveScore(music.title, score)
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
                            // 기존 데이터셋을 완전히 제거하고 새로 추가해야 재시작 시 정상 동작함
                            val userDataSet = LineDataSet(mutableListOf(), "User Pitch").apply {
                                color = android.graphics.Color.BLUE
                                setDrawCircles(false)
                                setDrawValues(false)
                                lineWidth = 2f
                            }

                            val originalDataSet = LineDataSet(mutableListOf(), "Original Pitch").apply {
                                color = android.graphics.Color.rgb(255, 165, 0)
                                setDrawCircles(false)
                                setDrawValues(false)
                                lineWidth = 2f
                            }

                            binding.pitchChart.data = LineData(userDataSet, originalDataSet)
                            binding.pitchChart.invalidate()

                            lastOriginIndex = -1
                            lastUserX = -1f
                            lastOriginX = -1f

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

    private var lastUserX = -1f
    private var lastOriginX = -1f

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

        if (xSec <= lastOriginX) return  // 역순 방지
        lastOriginX = xSec

        originDataSet.addEntry(Entry(xSec, originalPitch))
        adjustYAxisIfNeeded(originalPitch)
        trimAndRefreshChart(data)
    }

    private fun adjustYAxisIfNeeded(newPitch: Float) {
        val axis = binding.pitchChart.axisLeft
        if (newPitch > axis.axisMaximum) {
            axis.axisMaximum = (newPitch * 1.1f).coerceAtLeast(100f)
        }
    }

    private fun trimAndRefreshChart(data: LineData) {
        val chart = binding.pitchChart
        val userDataSet = data.getDataSetByLabel("User Pitch", true) ?: return
        val originDataSet = data.getDataSetByLabel("Original Pitch", true) ?: return

        if (userDataSet.entryCount > 100 && userDataSet.entryCount > 1) {
            userDataSet.removeFirst()
        }
        if (originDataSet.entryCount > 100 && originDataSet.entryCount > 1) {
            originDataSet.removeFirst()
        }

        if (userDataSet.entryCount < 2 || originDataSet.entryCount < 2) return

        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(10f)

        val lastX = userDataSet.getEntryForIndex(userDataSet.entryCount - 1).x
        chart.moveViewToX((lastX - 9f).coerceAtLeast(0f))

        chart.invalidate()
    }

    private fun initPitchChart() {
        val maxPitch = pitchArray.maxOrNull() ?: 1000f

        val chart = binding.pitchChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = (maxPitch * 1.1f).coerceAtLeast(100f)

        val xAxis = chart.xAxis
        xAxis.isEnabled = true
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = 5
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "%.0f초".format(value)
        }

        val userDataSet = LineDataSet(mutableListOf(), "User Pitch").apply {
            color = android.graphics.Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val originalDataSet = LineDataSet(mutableListOf(), "Original Pitch").apply {
            color = android.graphics.Color.rgb(255, 165, 0)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
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
