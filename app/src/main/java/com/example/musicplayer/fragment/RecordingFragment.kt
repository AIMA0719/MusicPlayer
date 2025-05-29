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
import com.example.musicplayer.viewModel.RecordingViewModel
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

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        initPitchChart()

        // 마이크 아이콘 클릭 시 녹음 시작 / 중지
        binding.micImage.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                viewModel.stopRecording()
            } else {
                viewModel.startRecording(pitchArray)
            }
        }

        val durationMillis = arguments?.getLong("durationMillis") ?: 0L
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        viewModel.elapsedTime.observe(viewLifecycleOwner) { elapsedMs ->
            val elapsed = formatMillisToTime(elapsedMs)
            val total = formatMillisToTime(durationMillis)
            binding.timeDisplay.text = "$elapsed / $total"
        }

        // pitch 및 오차 실시간 UI 반영
        viewModel.currentPitch.observe(viewLifecycleOwner) { pitch ->
            val diff = viewModel.pitchDifference.value ?: 0f
            val elapsed = viewModel.elapsedTime.value ?: 0
            val index = (elapsed / 100).toInt()
            val origin = if (index in pitchArray.indices) pitchArray[index] else 0f

            binding.pitchDifference.text = "🎵 현재 pitch: %.2f Hz / 오차: %.2f Hz".format(pitch, diff)
            addPitchEntry(pitch, origin) // ✅ 두 개의 pitch를 함께 전달
        }

        viewModel.isRecording.observe(viewLifecycleOwner) { recording ->
            binding.micImage.alpha = if (recording) 1.0f else 0.5f
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            Toast.makeText(requireContext(), "점수: $score 점", Toast.LENGTH_LONG).show()
        }

        return binding.root
    }

    private fun addPitchEntry(userPitch: Float, originalPitch: Float) {
        val chart = binding.pitchChart
        val data = chart.data ?: return

        val userDataSet = data.getDataSetByIndex(0)
        val originDataSet = data.getDataSetByIndex(1)

        val elapsedMs = viewModel.elapsedTime.value ?: 0L
        val xSec = elapsedMs / 1000f // ✅ x축을 초 단위로 사용

        userDataSet.addEntry(Entry(xSec, userPitch))
        originDataSet.addEntry(Entry(xSec, originalPitch))

        // 오래된 값 제거: 100초 이상이면 제거
        if (userDataSet.entryCount > 100) {
            userDataSet.removeFirst()
            originDataSet.removeFirst()
        }

        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(10f) // 최근 10초만 보이게
        chart.moveViewToX(xSec)
        chart.invalidate()
    }

    private fun initPitchChart() {
        val chart = binding.pitchChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 1000f

        val xAxis = chart.xAxis
        xAxis.isEnabled = false

        val userDataSet = LineDataSet(mutableListOf(), "User Pitch").apply {
            color = android.graphics.Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val originalDataSet = LineDataSet(mutableListOf(), "Original Pitch").apply {
            color = android.graphics.Color.rgb(255, 165, 0) // ORANGE
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
