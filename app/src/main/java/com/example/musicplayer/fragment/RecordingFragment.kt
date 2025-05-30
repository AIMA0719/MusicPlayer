package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentRecordingBinding
import com.example.musicplayer.manager.LogManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class RecordingFragment : Fragment() {

    private val viewModel: RecordingViewModel by viewModels()

    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray
    private var durationMillis: Long = 0

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = it.getParcelable("music")!!
            pitchArray = it.getFloatArray("pitchArray")!!
            durationMillis = it.getLong("durationMillis", 0L)
            LogManager.e(listOf(pitchArray.toList()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        val maxPitch = pitchArray.maxOrNull() ?: 1000f
        initPitchChart(maxPitch) // 최대값 기반으로 차트 세팅
        setObserver()

        // 마이크 아이콘 클릭 시 녹음 시작 / 중지
        binding.micImage.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                viewModel.stopRecording()
            } else {
                viewModel.startRecording(pitchArray)
            }
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun setObserver() {
        val durationMillis = arguments?.getLong("durationMillis") ?: 0L
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        viewModel.elapsedTime.observe(viewLifecycleOwner) { elapsedMs ->
            val clampedElapsed = elapsedMs.coerceAtMost(durationMillis)
            val elapsed = formatMillisToTime(clampedElapsed)
            val total = formatMillisToTime(durationMillis)
            binding.timeDisplay.text = "$elapsed / $total"

            // 오리지널 피치 추가
            val index = (elapsedMs / 100).toInt()
            if (index in pitchArray.indices) {
                val origin = pitchArray[index]
                addOriginPitchEntry(origin, elapsedMs / 1000f)
            }

            // 녹음이 끝나면 자동으로 중지
            if (elapsedMs >= durationMillis) {
                viewModel.stopRecording()
            }
        }

        // pitch 및 오차 실시간 UI 반영
        viewModel.currentPitch.observe(viewLifecycleOwner) { pitch ->
            val diff = viewModel.pitchDifference.value ?: 0f
            val elapsed = viewModel.elapsedTime.value ?: 0

            binding.pitchDifference.text = "🎵 현재 pitch: %.2f Hz / 오차: %.2f Hz".format(pitch, diff)

            addUserPitchEntry(pitch, elapsed / 1000f)
        }

        viewModel.isRecording.observe(viewLifecycleOwner) { recording ->
            binding.micImage.alpha = if (recording) 1.0f else 0.5f
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            Toast.makeText(requireContext(), "점수: $score 점", Toast.LENGTH_LONG).show()
        }

        viewModel.clearChartTrigger.observe(viewLifecycleOwner) {
            val chart = binding.pitchChart
            chart.data?.dataSets?.forEach { it.clear() }
            chart.data?.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
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
            val newMax = (newPitch * 1.1f).coerceAtLeast(100f)
            axis.axisMaximum = newMax
            binding.pitchChart.invalidate()
        }
    }

    private fun trimAndRefreshChart(data: LineData) {
        val chart = binding.pitchChart
        val userDataSet = data.getDataSetByIndex(0)
        val originDataSet = data.getDataSetByIndex(1)

        // 오래된 데이터 삭제
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
        leftAxis.axisMaximum = (maxPitch * 1.1f).coerceAtLeast(100f) // 약간 여유 있게

        val xAxis = chart.xAxis
        xAxis.isEnabled = true
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f  // 1초 단위 간격
        xAxis.labelCount = 5    // 라벨 개수 고정
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "%.0f초".format(value)
            }
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

        val data = LineData(userDataSet, originalDataSet)
        chart.data = data
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
        viewModel.stopRecording()
        _binding = null
    }

    companion object {
        fun newInstance(music: MusicFile, originalPitch: FloatArray, durationMillis: Long): RecordingFragment {
            val fragment = RecordingFragment()
            fragment.arguments = bundleOf(
                "music" to music,
                "pitchArray" to originalPitch,
                "durationMillis" to durationMillis
            )
            return fragment
        }

    }
}
