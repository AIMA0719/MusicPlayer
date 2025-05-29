package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.musicplayer.viewModel.RecordingViewModel
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentRecordingBinding
import com.example.musicplayer.manager.LogManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Arrays

class RecordingFragment : Fragment() {

    private val viewModel: RecordingViewModel by viewModels()

    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = it.getParcelable("music")!!
            pitchArray = it.getFloatArray("pitchArray")!!
            LogManager.e(listOf(pitchArray.toList()))
        }
    }

    @SuppressLint("DefaultLocale")
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

        binding.btnStopRecording.setOnClickListener {
            viewModel.stopRecording()
        }

        viewModel.elapsedTime.observe(viewLifecycleOwner) { ms ->
            val seconds = ms / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            binding.elapsedTime.text = String.format("%02d:%02d", minutes, remainingSeconds)
        }

        // pitch 및 오차 실시간 UI 반영
        viewModel.currentPitch.observe(viewLifecycleOwner) { pitch ->
            val diff = viewModel.pitchDifference.value ?: 0f
            val elapsed = viewModel.elapsedTime.value ?: 0
            val index = elapsed / 100
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

        val entryCount = userDataSet.entryCount
        userDataSet.addEntry(Entry(entryCount.toFloat(), userPitch))
        originDataSet.addEntry(Entry(entryCount.toFloat(), originalPitch))

        // 오래된 값 제거
        if (entryCount > 100) {
            userDataSet.removeFirst()
            originDataSet.removeFirst()

            // x 값 재정렬
            for (i in 0 until userDataSet.entryCount) {
                userDataSet.getEntryForIndex(i).x = i.toFloat()
                originDataSet.getEntryForIndex(i).x = i.toFloat()
            }
        }

        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(100f)
        chart.moveViewToX(data.entryCount.toFloat())
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



    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopRecording()
        _binding = null
    }

    companion object {
        fun newInstance(music: MusicFile, pitchArray: FloatArray): RecordingFragment {
            val fragment = RecordingFragment()
            val args = Bundle().apply {
                putParcelable("music", music)
                putFloatArray("pitchArray", pitchArray) // ✅ 동일한 키로 통일
            }
            fragment.arguments = args
            return fragment
        }
    }
}
