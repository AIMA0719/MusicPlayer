package com.example.musicplayer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.databinding.FragmentMicTestBinding
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * 마이크 테스트 화면
 *
 * - 실시간 음량 레벨 표시
 * - 피치 감지 테스트
 * - 마이크 상태 확인
 */
class MicTestFragment : Fragment() {

    private var _binding: FragmentMicTestBinding? = null
    private val binding get() = _binding!!

    private var audioDispatcher: AudioDispatcher? = null
    private var testJob: Job? = null
    private var isTesting = false

    // 마이크 권한 요청
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMicTest()
        } else {
            ToastManager.showToast("마이크 권한이 필요합니다")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMicTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // 테스트 시작/중지 버튼
        binding.btnStartTest.setOnClickListener {
            if (isTesting) {
                stopMicTest()
            } else {
                checkPermissionAndStart()
            }
        }

        // 초기 상태
        updateUI(isRecording = false)
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startMicTest()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startMicTest() {
        if (isTesting) return

        isTesting = true
        updateUI(isRecording = true)
        binding.tvStatus.text = "마이크 테스트 중..."

        testJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 오디오 디스패처 생성
                audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

                // 피치 감지 프로세서
                val pitchProcessor = PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.YIN,
                    22050f,
                    1024
                ) { result, event ->
                    if (!isActive) return@PitchProcessor

                    val pitch = result.pitch
                    val amplitude = event.rms.toDouble()
                    val dbLevel = if (amplitude > 0) 20 * log10(amplitude) else -100.0

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (_binding != null) {
                            updateAudioDisplay(pitch, dbLevel)
                        }
                    }
                }

                audioDispatcher?.addAudioProcessor(pitchProcessor)
                audioDispatcher?.run()

            } catch (e: Exception) {
                LogManager.e("Mic test error: ${e.message}")
                withContext(Dispatchers.Main) {
                    ToastManager.showToast("마이크 테스트 실패: ${e.message}")
                    stopMicTest()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateAudioDisplay(pitch: Float, dbLevel: Double) {
        if (_binding == null) return

        // 음량 레벨 (dB -> 0-100 스케일)
        val normalizedLevel = ((dbLevel + 60) / 60 * 100).coerceIn(0.0, 100.0).toInt()
        binding.progressVolume.progress = normalizedLevel
        binding.tvVolumeValue.text = "${String.format("%.1f", dbLevel)} dB"

        // 피치 표시
        if (pitch > 0) {
            val note = pitchToNote(pitch)
            binding.tvPitchValue.text = "${String.format("%.1f", pitch)} Hz ($note)"

            // 음량이 충분하면 감지 성공
            if (normalizedLevel > 20) {
                binding.tvStatus.text = "마이크 정상 작동 중"
                binding.ivMicStatus.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                )
            }
        } else {
            binding.tvPitchValue.text = "-- Hz"
        }

        // 음량 상태 아이콘
        val volumeIcon = when {
            normalizedLevel > 70 -> android.R.color.holo_red_light      // 너무 큼
            normalizedLevel > 40 -> android.R.color.holo_green_light    // 적절
            normalizedLevel > 20 -> android.R.color.holo_orange_light   // 작음
            else -> android.R.color.darker_gray                          // 감지 안됨
        }
        binding.progressVolume.progressTintList = ContextCompat.getColorStateList(requireContext(), volumeIcon)
    }

    /**
     * 주파수를 음계로 변환
     */
    private fun pitchToNote(frequency: Float): String {
        if (frequency <= 0) return "--"

        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val a4: Double = 440.0
        val c0: Double = a4 * 2.0.pow(-4.75)

        val freqDouble: Double = frequency.toDouble()
        val h: Double = 12.0 * (log10(freqDouble / c0) / log10(2.0))
        val octave: Int = (h / 12.0).toInt()
        val n: Int = (h % 12.0).toInt()

        return if (n >= 0 && n < notes.size) {
            "${notes[n]}$octave"
        } else {
            "--"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun stopMicTest() {
        isTesting = false
        testJob?.cancel()
        testJob = null

        audioDispatcher?.stop()
        audioDispatcher = null

        updateUI(isRecording = false)
        binding.tvStatus.text = "테스트 대기 중"
        binding.tvPitchValue.text = "-- Hz"
        binding.tvVolumeValue.text = "-- dB"
        binding.progressVolume.progress = 0
        binding.ivMicStatus.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
    }

    private fun updateUI(isRecording: Boolean) {
        binding.btnStartTest.text = if (isRecording) "테스트 중지" else "테스트 시작"
        binding.btnStartTest.setIconResource(
            if (isRecording) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMicTest()
        _binding = null
    }

    companion object {
        fun newInstance() = MicTestFragment()
    }
}
