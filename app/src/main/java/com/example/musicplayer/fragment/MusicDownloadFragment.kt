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
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.manager.IOSStyleProgressDialog
import com.example.musicplayer.repository.YouTubeRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MusicDownloadFragment : Fragment() {

    private var _binding: FragmentMusicDownloadBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var youTubeRepository: YouTubeRepository

    private lateinit var downloadAdapter: MusicDownloadAdapter
    private lateinit var downloadedAdapter: DownloadedFileAdapter
    private lateinit var downloadManager: FileDownloadManager
    private lateinit var progressDialog: IOSStyleProgressDialog

    private var mediaPlayer: MediaPlayer? = null
    private var currentDownloadId: String? = null
    private var isLoading = false
    private var currentTab = Tab.DOWNLOAD

    private enum class Tab {
        DOWNLOAD, DOWNLOADED
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ToastManager.showToast("저장소 권한이 허용되었습니다")
        } else {
            ToastManager.showToast("저장소 권한이 필요합니다")
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = GoogleAuthManager.handleSignInResult(task)
        if (account != null) {
            ToastManager.showToast("로그인 성공: ${account.email}")
            loadMusicFromApi()
        } else {
            ToastManager.showToast("로그인이 필요합니다")
        }
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

        downloadManager = FileDownloadManager()
        progressDialog = IOSStyleProgressDialog(requireContext())

        setupAdapters()
        setupViews()
        observeDownloadProgress()
        checkStoragePermission()

        loadMusicFromApi()
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
        }

        binding.btnCancelDownload.setOnClickListener {
            currentDownloadId?.let { downloadManager.cancelDownload(it) }
            binding.layoutDownloadProgress.isVisible = false
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab

        when (tab) {
            Tab.DOWNLOAD -> {
                binding.btnTabDownload.setBackgroundResource(android.R.drawable.btn_default)
                binding.btnTabDownloaded.setBackgroundResource(android.R.drawable.btn_default)
                binding.rvDownloadList.isVisible = true
                binding.rvDownloadedList.isVisible = false

                // 다운로드 탭의 빈 메시지 업데이트
                val downloadList = downloadAdapter.currentList
                binding.tvEmptyMessage.apply {
                    text = "다운로드 가능한 음악이 없습니다"
                    isVisible = downloadList.isEmpty()
                }
            }
            Tab.DOWNLOADED -> {
                binding.btnTabDownload.setBackgroundResource(android.R.drawable.btn_default)
                binding.btnTabDownloaded.setBackgroundResource(android.R.drawable.btn_default)
                binding.rvDownloadList.isVisible = false
                binding.rvDownloadedList.isVisible = true

                // 다운로드됨 탭으로 전환 시 파일 로드
                loadDownloadedFiles()
            }
        }
    }

    private fun loadMusicFromApi() {
        if (isLoading) return
        isLoading = true

        viewLifecycleOwner.lifecycleScope.launch {
            progressDialog.show("음악 목록 로딩 중...")
            try {
                // Google 계정 확인
                val account = GoogleAuthManager.getLastSignedInAccount(requireContext())
                if (account == null) {
                    progressDialog.dismiss()
                    // 로그인 필요
                    val signInIntent = GoogleAuthManager.getSignInIntent()
                    signInLauncher.launch(signInIntent)
                    return@launch
                }

                // YouTube API에서 음악 검색
                val musicList = withContext(Dispatchers.IO) {
                    try {
                        val response = youTubeRepository.searchVideos("노래방 인기곡")
                        if (response.isSuccessful && response.body() != null) {
                            val jsonResponse = response.body()!!
                            val items = jsonResponse.getAsJsonArray("items")

                            items?.mapNotNull { itemElement ->
                                try {
                                    val item = itemElement.asJsonObject
                                    val snippet = item.getAsJsonObject("snippet")
                                    val videoId = item.getAsJsonObject("id")?.get("videoId")?.asString

                                    if (videoId != null) {
                                        val thumbnailUrl = snippet.getAsJsonObject("thumbnails")
                                            ?.getAsJsonObject("medium")
                                            ?.get("url")?.asString ?: ""

                                        MusicDownloadItem(
                                            id = videoId,
                                            title = snippet.get("title").asString,
                                            artist = snippet.get("channelTitle").asString,
                                            url = "https://www.youtube.com/watch?v=$videoId",
                                            duration = "N/A",
                                            size = "N/A",
                                            genre = "YouTube",
                                            thumbnailUrl = thumbnailUrl,
                                            imageUrl = if (thumbnailUrl.isEmpty()) null else thumbnailUrl
                                        )
                                    } else null
                                } catch (e: Exception) {
                                    LogManager.e("Failed to parse item: ${e.message}")
                                    null
                                }
                            } ?: emptyList()
                        } else {
                            LogManager.e("YouTube API failed: ${response.code()}")
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("YouTube API error: ${e.message}")
                        emptyList()
                    }
                }

                downloadAdapter.submitList(musicList)
                binding.tvEmptyMessage.isVisible = musicList.isEmpty()
                progressDialog.dismiss()
                isLoading = false
            } catch (e: Exception) {
                LogManager.e("Failed to load music from API: ${e.message}")
                ToastManager.showToast("음악 목록 로드 실패: ${e.message}")
                progressDialog.dismiss()
                isLoading = false
            }
        }
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
        if (musicItem.genre == "YouTube") {
            ToastManager.showToast("YouTube 다운로드는 지원되지 않습니다 (API 제한)")
            return
        }

        val fileName = "${musicItem.title.replace(" ", "_")}_${musicItem.artist.replace(" ", "_")}.mp3"

        currentDownloadId = musicItem.id
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
        if (progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
        _binding = null
    }

    companion object {
        fun newInstance(): MusicDownloadFragment {
            return MusicDownloadFragment()
        }
    }
}