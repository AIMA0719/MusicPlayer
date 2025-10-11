package com.example.musicplayer.fragment

import android.Manifest
import android.content.Intent
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
import com.example.musicplayer.manager.AudioRecorderManager
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class RecordingOnlyFragment : Fragment() {

    private var _binding: FragmentRecordingOnlyBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioRecorderManager: AudioRecorderManager
    private var currentRecordingFile: String? = null

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
        setupViews()
        observeRecordingState()
    }

    private fun setupAudioRecorder() {
        audioRecorderManager = AudioRecorderManager()
    }

    private fun setupViews() {
        binding.btnRecord.setOnClickListener {
            if (audioRecorderManager.isRecording.value) {
                stopRecording()
            } else {
                checkPermissionAndStartRecording()
            }
        }

        binding.btnPause.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                audioRecorderManager.pauseRecording()
                binding.btnPause.text = "재개"
                binding.tvStatus.text = "녹음 일시정지"
            } else {
                ToastManager.showToast("Android 7.0 이상에서 지원됩니다.")
            }
        }

        binding.btnStop.setOnClickListener {
            stopRecording()
        }

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
                    audioRecorderManager.recordingTime.collectLatest { time ->
                        binding.tvRecordingTime.text = AudioRecorderManager.formatTime(time)
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
            binding.btnRecord.text = "녹음\n중지"
            binding.btnRecord.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            binding.tvStatus.text = "녹음 중..."
            binding.viewRecordingIndicator.isVisible = true
            binding.btnPause.isVisible = true
            binding.btnStop.isVisible = true
            binding.layoutSavedFile.isVisible = false
            
            // 마이크 아이콘을 빨간색으로 변경
            binding.ivMicrophone.setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
        } else {
            // 녹음 정지
            binding.btnRecord.text = "녹음\n시작"
            binding.btnRecord.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
            binding.tvStatus.text = "녹음 대기 중"
            binding.viewRecordingIndicator.isVisible = false
            binding.btnPause.isVisible = false
            binding.btnStop.isVisible = false
            
            // 마이크 아이콘을 원래 색상으로 복원
            binding.ivMicrophone.clearColorFilter()
            
            // 일시정지 버튼 텍스트 초기화
            binding.btnPause.text = "일시\n정지"
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
                ToastManager.showToast("녹음이 저장되었습니다.")
            } else {
                ToastManager.showToast("녹음 저장에 실패했습니다.")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to stop recording: ${e.message}")
            ToastManager.showToast("녹음 정지 실패: ${e.message}")
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
            audioRecorderManager.stopRecording()
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