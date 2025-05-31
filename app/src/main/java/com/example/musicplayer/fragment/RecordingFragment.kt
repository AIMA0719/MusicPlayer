package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentRecordingBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordingFragment : Fragment() {

    private val viewModel: RecordingViewModel by viewModels()

    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray
    private var durationMillis: Long = 0

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private var elapsedJob: Job? = null
    private var lastOriginIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = it.getParcelable("music")!!
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
            if (viewModel.isRecording.value == true) {
                stopElapsedTimer()
                viewModel.stopRecording()
            } else {
                viewModel.startRecording(pitchArray)
                startElapsedTimer()
            }
        }

        return binding.root
    }

    private fun startElapsedTimer() {
        val startTime = SystemClock.elapsedRealtime()
        elapsedJob = lifecycleScope.launch {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - startTime
                viewModel.elapsedTime.postValue(elapsed)
                delay(100)
            }
        }
    }

    private fun stopElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = null
    }

    @SuppressLint("SetTextI18n")
    private fun setObserver() {
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        //LogManager.e(listOf(pitchArray.toList()))

        viewModel.elapsedTime.observe(viewLifecycleOwner) { elapsedMs ->
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

            if (clampedElapsed >= durationMillis) {
                stopElapsedTimer()
                viewModel.stopRecording()
            }
        }

        viewModel.currentPitch.observe(viewLifecycleOwner) { pitch ->
            val diff = viewModel.pitchDifference.value ?: 0f
            val elapsed = viewModel.elapsedTime.value ?: 0

            binding.pitchDifference.text = "\uD83C\uDFB5 현재 pitch: %.2f Hz / 오차: %.2f Hz".format(pitch, diff)
            addUserPitchEntry(pitch, elapsed / 1000f)
        }

        viewModel.isRecording.observe(viewLifecycleOwner) { recording ->
            binding.micImage.alpha = if (recording) 1.0f else 0.5f
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            Toast.makeText(requireContext(), "점수: $score 점", Toast.LENGTH_LONG).show()
        }

        viewModel.clearChartTrigger.observe(viewLifecycleOwner) {
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
        val totalSeconds = (millis / 1000.0).toInt() + if (millis % 1000 > 0) 1 else 0
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopElapsedTimer()
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
