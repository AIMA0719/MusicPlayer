package com.example.musicplayer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.adapter.DownloadedFileAdapter
import com.example.musicplayer.adapter.MusicDownloadAdapter
import com.example.musicplayer.data.MusicDownloadItem
import com.example.musicplayer.databinding.FragmentMusicDownloadBinding
import com.example.musicplayer.manager.FileDownloadManager
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.server.JamendoApiService
import com.example.musicplayer.server.RetrofitClient
import com.example.musicplayer.server.model.toMusicDownloadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicDownloadFragment : Fragment() {

    private var _binding: FragmentMusicDownloadBinding? = null
    private val binding get() = _binding!!

    private lateinit var downloadAdapter: MusicDownloadAdapter
    private lateinit var downloadedAdapter: DownloadedFileAdapter
    private lateinit var downloadManager: FileDownloadManager
    private lateinit var jamendoApi: JamendoApiService

    private var currentTab = Tab.DOWNLOAD
    private var mediaPlayer: MediaPlayer? = null
    private var isLoading = false

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            ToastManager.showToast("저장소 권한이 필요합니다.")
        }
    }

    enum class Tab {
        DOWNLOAD, DOWNLOADED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDownloadManager()
        setupApiService()
        setupAdapters()
        setupViews()

        // 초기 탭 설정
        switchTab(Tab.DOWNLOAD)

        // API에서 음악 목록 로드
        loadMusicFromApi()
        observeDownloadProgress()
        checkStoragePermission()
    }

    private fun setupDownloadManager() {
        downloadManager = FileDownloadManager()
    }

    private fun setupApiService() {
        jamendoApi = RetrofitClient.createService()
    }

    private fun setupAdapters() {
        downloadAdapter = MusicDownloadAdapter { musicItem ->
            startDownload(musicItem)
        }

        downloadedAdapter = DownloadedFileAdapter(
            onPlayClick = { file ->
                playAudioFile(file)
            },
            onDeleteClick = { file ->
                deleteFile(file)
            }
        )

        binding.rvDownloadList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadAdapter
        }

        binding.rvDownloadedList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadedAdapter
        }
    }

    private fun setupViews() {
        binding.btnTabDownload.setOnClickListener {
            switchTab(Tab.DOWNLOAD)
        }

        binding.btnTabDownloaded.setOnClickListener {
            switchTab(Tab.DOWNLOADED)
            loadDownloadedFiles()
        }

        binding.btnCancelDownload.setOnClickListener {
            // 현재 다운로드 중인 것이 있다면 취소
            downloadManager.clearCompletedDownloads()
            binding.layoutDownloadProgress.isVisible = false
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        
        when (tab) {
            Tab.DOWNLOAD -> {
                // 탭 버튼 색상 변경
                binding.btnTabDownload.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                binding.btnTabDownloaded.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
                
                // 리스트 표시/숨김
                binding.rvDownloadList.isVisible = true
                binding.rvDownloadedList.isVisible = false
                
                // 다운로드 가능한 음악 리스트 확인
                val downloadList = downloadAdapter.currentList
                binding.tvEmptyMessage.apply {
                    text = "다운로드 가능한 음악이 없습니다"
                    isVisible = downloadList.isEmpty()
                }
            }
            Tab.DOWNLOADED -> {
                // 탭 버튼 색상 변경
                binding.btnTabDownload.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
                binding.btnTabDownloaded.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                
                // 리스트 표시/숨김
                binding.rvDownloadList.isVisible = false
                binding.rvDownloadedList.isVisible = true
                
                // 다운로드된 파일 리스트 확인
                val downloadedFiles = downloadedAdapter.currentList
                binding.tvEmptyMessage.apply {
                    text = "다운로드된 파일이 없습니다"
                    isVisible = downloadedFiles.isEmpty()
                }
            }
        }
    }

    /**
     * Jamendo API에서 인기 음악 목록 로드
     */
    private fun loadMusicFromApi() {
        if (isLoading) return

        isLoading = true
        binding.tvEmptyMessage.apply {
            text = "음악 목록을 불러오는 중..."
            isVisible = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    jamendoApi.getTracks(
                        limit = 30,
                        order = "popularity_week"
                    )
                }

                if (response.isSuccessful) {
                    val tracksResponse = response.body()
                    if (tracksResponse != null && tracksResponse.headers.status == "success") {
                        val musicList = tracksResponse.results.map { it.toMusicDownloadItem() }
                        downloadAdapter.submitList(musicList)

                        // 현재 탭이 DOWNLOAD일 때만 빈 메시지 업데이트
                        if (currentTab == Tab.DOWNLOAD) {
                            binding.tvEmptyMessage.apply {
                                text = "다운로드 가능한 음악이 없습니다"
                                isVisible = musicList.isEmpty()
                            }
                        }

                        LogManager.i("Loaded ${musicList.size} tracks from Jamendo API")
                    } else {
                        showApiError("API 응답 오류: ${tracksResponse?.headers?.errorMessage}")
                    }
                } else {
                    showApiError("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                LogManager.e("Failed to load music from API: ${e.message}")
                showApiError("음악 목록을 불러오지 못했습니다: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 특정 장르의 음악 로드
     */
    private fun loadMusicByGenre(genre: String) {
        if (isLoading) return

        isLoading = true
        binding.tvEmptyMessage.apply {
            text = "음악 목록을 불러오는 중..."
            isVisible = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    jamendoApi.getTracksByGenre(
                        tags = genre,
                        limit = 30
                    )
                }

                if (response.isSuccessful) {
                    val tracksResponse = response.body()
                    if (tracksResponse != null && tracksResponse.headers.status == "success") {
                        val musicList = tracksResponse.results.map { it.toMusicDownloadItem() }
                        downloadAdapter.submitList(musicList)

                        if (currentTab == Tab.DOWNLOAD) {
                            binding.tvEmptyMessage.apply {
                                text = "다운로드 가능한 음악이 없습니다"
                                isVisible = musicList.isEmpty()
                            }
                        }

                        LogManager.i("Loaded ${musicList.size} tracks for genre: $genre")
                    } else {
                        showApiError("API 응답 오류: ${tracksResponse?.headers?.errorMessage}")
                    }
                } else {
                    showApiError("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                LogManager.e("Failed to load music by genre: ${e.message}")
                showApiError("음악 목록을 불러오지 못했습니다: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun showApiError(message: String) {
        binding.tvEmptyMessage.apply {
            text = message
            isVisible = true
        }
        ToastManager.showToast(message)
    }

    private fun loadDownloadedFiles() {
        val downloadedFiles = downloadManager.getDownloadedFiles(requireContext())
        downloadedAdapter.submitList(downloadedFiles)
        
        // 현재 탭이 DOWNLOADED일 때만 빈 메시지 업데이트
        if (currentTab == Tab.DOWNLOADED) {
            binding.tvEmptyMessage.apply {
                text = "다운로드된 파일이 없습니다"
                isVisible = downloadedFiles.isEmpty()
            }
        }
    }

    private fun startDownload(musicItem: MusicDownloadItem) {
        val fileName = "${musicItem.title.replace(" ", "_")}_${musicItem.artist.replace(" ", "_")}.mp3"
        
        binding.layoutDownloadProgress.isVisible = true
        binding.tvDownloadFile.text = fileName
        
        downloadManager.downloadFile(
            context = requireContext(),
            downloadId = musicItem.id,
            url = musicItem.url,
            fileName = fileName
        )
        
        ToastManager.showToast("다운로드를 시작합니다")
    }

    private fun observeDownloadProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadManager.downloadProgress.collectLatest { progressMap ->
                    if (progressMap.isNotEmpty()) {
                        val progress = progressMap.values.first()
                        updateProgressUI(progress)
                    } else {
                        binding.layoutDownloadProgress.isVisible = false
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgressUI(progress: com.example.musicplayer.manager.DownloadProgress) {
        binding.apply {
            progressDownload.progress = progress.progress
            tvDownloadProgress.text = "${progress.progress}%"
            
            val downloadedSize = FileDownloadManager.formatFileSize(progress.downloadedBytes)
            val totalSize = FileDownloadManager.formatFileSize(progress.totalBytes)
            tvDownloadSize.text = "$downloadedSize / $totalSize"
            
            if (progress.isCompleted) {
                ToastManager.showToast("다운로드가 완료되었습니다!")
                layoutDownloadProgress.isVisible = false
                
                // 다운로드된 파일 리스트 항상 새로고침 (내부 데이터 업데이트)
                loadDownloadedFiles()
            } else if (progress.isError) {
                ToastManager.showToast("다운로드 실패: ${progress.errorMessage}")
                layoutDownloadProgress.isVisible = false
            }
        }
    }

    private fun playAudioFile(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
            ToastManager.showToast("재생 시작: ${file.name}")
        } catch (e: Exception) {
            LogManager.e("Failed to play audio: ${e.message}")
            ToastManager.showToast("재생 실패: ${e.message}")
        }
    }

    private fun deleteFile(file: File) {
        try {
            if (file.delete()) {
                ToastManager.showToast("파일이 삭제되었습니다")
                loadDownloadedFiles()
            } else {
                ToastManager.showToast("파일 삭제에 실패했습니다")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to delete file: ${e.message}")
            ToastManager.showToast("파일 삭제 오류: ${e.message}")
        }
    }

    private fun checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        downloadManager.release()
        _binding = null
    }

    companion object {
        fun newInstance(): MusicDownloadFragment {
            return MusicDownloadFragment()
        }
    }
}