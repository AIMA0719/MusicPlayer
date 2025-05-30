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
        val maxPitch = pitchArray.maxOrNull() ?: 1000f
        initPitchChart(maxPitch)
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
            binding.pitchChart.data?.dataSets?.forEach { it.clear() }
            binding.pitchChart.data?.notifyDataChanged()
            binding.pitchChart.notifyDataSetChanged()
            lastOriginIndex = -1
        }
    }

    private fun addUserPitchEntry(userPitch: Float, xSec: Float) {
        val chart = binding.pitchChart
        val data = chart.data ?: return
        val userDataSet = data.getDataSetByIndex(0)
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
        val userDataSet = data.getDataSetByIndex(0)
        val originDataSet = data.getDataSetByIndex(1)

        if (userDataSet.entryCount > 100) userDataSet.removeFirst()
        if (originDataSet.entryCount > 100) originDataSet.removeFirst()

        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(10f)

        if (userDataSet.entryCount > 0) {
            val lastX = userDataSet.getEntryForIndex(userDataSet.entryCount - 1).x
            chart.moveViewToX((lastX - 9f).coerceAtLeast(0f))
        }

        chart.invalidate()
    }

    private fun initPitchChart(maxPitch: Float = 1000f) {
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
