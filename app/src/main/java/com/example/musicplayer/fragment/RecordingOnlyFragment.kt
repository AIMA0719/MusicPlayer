package com.example.musicplayer.fragment

import android.Manifest
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentRecordingOnlyBinding
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.manager.AudioRecorderManager
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.manager.GameManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class RecordingOnlyFragment : Fragment() {

    private var _binding: FragmentRecordingOnlyBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var gameManager: GameManager
    private var gameManagerInitJob: Job? = null
    private var currentRecordingFile: String? = null
    private var recordingStartTime: Long = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            ToastManager.showToast("녹음 권한이 필요합니다.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingOnlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAudioRecorder()
        setupGameManager()
        setupViews()
        observeRecordingState()
    }

    private fun setupAudioRecorder() {
        audioRecorderManager = AudioRecorderManager()
    }

    private fun setupGameManager() {
        val userId = com.example.musicplayer.manager.AuthManager.getCurrentUserId() ?: "guest"
        gameManager = GameManager(requireContext(), userId)
        gameManagerInitJob = lifecycleScope.launch {
            gameManager.initialize()
        }
    }

    private fun setupViews() {
        // 녹음 시작 버튼
        binding.btnStart.setOnClickListener {
            checkPermissionAndStartRecording()
        }

        // 일시정지/재개 버튼
        binding.btnPause.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (audioRecorderManager.isPaused.value) {
                    // 일시정지 중이면 재개
                    audioRecorderManager.resumeRecording()
                } else {
                    // 녹음 중이면 일시정지
                    audioRecorderManager.pauseRecording()
                }
            } else {
                ToastManager.showToast("Android 7.0 이상에서 지원됩니다.")
            }
        }

        // 녹음 완료 버튼
        binding.btnStop.setOnClickListener {
            stopRecording()
        }

        // 공유 버튼
        binding.btnShare.setOnClickListener {
            shareRecordingFile()
        }
    }

    private fun observeRecordingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    audioRecorderManager.isRecording.collectLatest { isRecording ->
                        updateRecordingUI(isRecording)
                    }
                }

                launch {
                    audioRecorderManager.isPaused.collectLatest { isPaused ->
                        updatePauseUI(isPaused)
                    }
                }

                launch {
                    audioRecorderManager.recordingTime.collectLatest { time ->
                        binding.tvRecordingTime.text = AudioRecorderManager.formatTime(time)
                    }
                }

                launch {
                    audioRecorderManager.amplitude.collectLatest { amplitude ->
                        binding.waveformCircleView.setAmplitude(amplitude)
                    }
                }

                launch {
                    audioRecorderManager.recordingError.collectLatest { error ->
                        error?.let {
                            ToastManager.showToast(it)
                            audioRecorderManager.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            // 녹음 중
            binding.tvStatus.text = "녹음 중..."
            binding.viewRecordingIndicator.isVisible = true

            // 버튼 visibility 변경
            binding.btnStart.isVisible = false
            binding.btnPause.isVisible = true
            binding.btnStop.isVisible = true

            binding.layoutSavedFile.isVisible = false

            // Circle View 애니메이션 시작
            binding.waveformCircleView.startRecording()
        } else {
            // 녹음 정지
            binding.tvStatus.text = "녹음 대기 중"
            binding.viewRecordingIndicator.isVisible = false

            // 버튼 visibility 변경
            binding.btnStart.isVisible = true
            binding.btnPause.isVisible = false
            binding.btnStop.isVisible = false

            // 일시정지 버튼 텍스트 초기화
            binding.btnPause.text = "⏸"
            binding.btnPause.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(), android.R.color.holo_orange_dark
            )

            // Circle View 애니메이션 중지
            binding.waveformCircleView.stopRecording()
        }
    }

    private fun updatePauseUI(isPaused: Boolean) {
        if (isPaused) {
            // 일시정지 중
            binding.btnPause.text = "▶"
            binding.btnPause.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(), android.R.color.holo_green_light
            )
            binding.tvStatus.text = "일시정지"
            binding.viewRecordingIndicator.isVisible = false

            // Circle View 일시정지 상태
            binding.waveformCircleView.pauseRecording()
        } else {
            // 녹음 중 또는 정지
            if (audioRecorderManager.isRecording.value) {
                binding.btnPause.text = "⏸"
                binding.btnPause.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_orange_dark
                )
                binding.tvStatus.text = "녹음 중..."
                binding.viewRecordingIndicator.isVisible = true

                // Circle View 재개 상태
                binding.waveformCircleView.resumeRecording()
            }
        }
    }

    private fun checkPermissionAndStartRecording() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        try {
            recordingStartTime = System.currentTimeMillis()
            currentRecordingFile = audioRecorderManager.startRecording(requireContext())
            if (currentRecordingFile != null) {
                LogManager.i("Recording started: $currentRecordingFile")
                binding.layoutSavedFile.isVisible = false
            } else {
                ToastManager.showToast("녹음을 시작할 수 없습니다.")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to start recording: ${e.message}")
            ToastManager.showToast("녹음 시작 실패: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            val savedFilePath = audioRecorderManager.stopRecording()
            if (savedFilePath != null) {
                currentRecordingFile = savedFilePath
                showSavedFileInfo(savedFilePath)
                LogManager.i("Recording saved: $savedFilePath")

                // 녹음 히스토리 저장 및 도전과제 체크
                saveRecordingHistory(savedFilePath)

                ToastManager.showToast("녹음이 저장되었습니다.")

                // MediaStore에 파일 등록 (선택사항, 시스템 미디어 스캔)
                scanMediaFile(savedFilePath)
            } else {
                ToastManager.showToast("녹음 저장에 실패했습니다.")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to stop recording: ${e.message}")
            ToastManager.showToast("녹음 정지 실패: ${e.message}")
        }
    }

    private fun saveRecordingHistory(filePath: String) {
        lifecycleScope.launch {
            try {
                // GameManager 초기화 완료 대기
                gameManagerInitJob?.join()

                LogManager.i("Starting to save recording history")

                val userId = com.example.musicplayer.manager.AuthManager.getCurrentUserId() ?: "guest"
                val recordingEndTime = System.currentTimeMillis()
                val duration = recordingEndTime - recordingStartTime

                // RecordingHistoryEntity 생성 (단순 녹음이므로 점수는 0)
                val recordingHistory = RecordingHistoryEntity(
                    userId = userId,
                    songName = "녹음 파일",
                    songArtist = "",
                    songDuration = duration,
                    totalScore = 0,
                    pitchAccuracy = 0.0,
                    rhythmScore = 0.0,
                    volumeStability = 0.0,
                    durationMatch = 0.0,
                    hasVibrato = false,
                    vibratoScore = 0.0,
                    difficulty = "NONE",
                    recordingFilePath = filePath
                )

                LogManager.i("Calling gameManager.onRecordingCompleted")

                // 게임 보상 계산 (녹음 개수, 도전과제 등)
                val reward = gameManager.onRecordingCompleted(
                    songName = "녹음 파일",
                    score = 0,
                    difficulty = "NONE",
                    recordingHistory = recordingHistory
                )

                LogManager.i("Recording history saved successfully. Exp gained: ${reward.exp}, Achievements: ${reward.unlockedAchievements.size}")
            } catch (e: Exception) {
                LogManager.e("Failed to save recording history: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * MediaStore에 파일 등록 (미디어 스캔)
     * 이렇게 하면 시스템 갤러리나 뮤직 앱에서도 파일을 볼 수 있습니다.
     */
    private fun scanMediaFile(filePath: String) {
        try {
            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(filePath),
                arrayOf("audio/*")
            ) { path, uri ->
                LogManager.i("Media scan completed: $path -> $uri")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to scan media file: ${e.message}")
        }
    }

    private fun showSavedFileInfo(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            binding.layoutSavedFile.isVisible = true
            binding.tvFilePath.text = "파일 경로: ${file.name}"
            
            val fileSizeMB = file.length() / (1024.0 * 1024.0)
            binding.tvFileSize.text = "파일 크기: ${"%.2f".format(fileSizeMB)} MB"
        }
    }

    private fun shareRecordingFile() {
        currentRecordingFile?.let { filePath ->
            try {
                val file = File(filePath)
                if (file.exists()) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        ))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "녹음 파일 공유"))
                } else {
                    ToastManager.showToast("파일을 찾을 수 없습니다.")
                }
            } catch (e: Exception) {
                LogManager.e("Failed to share file: ${e.message}")
                ToastManager.showToast("파일 공유 실패: ${e.message}")
            }
        } ?: ToastManager.showToast("공유할 파일이 없습니다.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (audioRecorderManager.isRecording.value) {
            // 녹음 중이면 우리의 stopRecording() 메서드 호출 (히스토리 저장 포함)
            stopRecording()
        }
        audioRecorderManager.release()
        _binding = null
    }

    companion object {
        fun newInstance(): RecordingOnlyFragment {
            return RecordingOnlyFragment()
        }
    }
}