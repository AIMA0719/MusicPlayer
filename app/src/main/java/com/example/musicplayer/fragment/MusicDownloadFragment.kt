package com.example.musicplayer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.DownloadedFileAdapter
import com.example.musicplayer.adapter.MusicDownloadAdapter
import com.example.musicplayer.data.MusicDownloadItem
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentMusicDownloadBinding
import com.example.musicplayer.factory.MusicFileDispatcherFactory
import com.example.musicplayer.manager.FileDownloadManager
import com.example.musicplayer.manager.IOSStyleProgressDialog
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.JamendoRepository
import com.google.android.material.tabs.TabLayout
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
    lateinit var jamendoRepository: JamendoRepository

    private lateinit var downloadAdapter: MusicDownloadAdapter
    private lateinit var downloadedAdapter: DownloadedFileAdapter
    private lateinit var downloadManager: FileDownloadManager
    private lateinit var progressDialog: IOSStyleProgressDialog

    private var currentDownloadId: String? = null
    private var isLoading = false
    private var isLoadingMore = false
    private var currentTab = Tab.DOWNLOAD
    private var currentFilter = "featured"
    private var currentSearchQuery: String? = null

    // 페이징 관련 변수
    private var currentOffset = 0
    private var hasMoreData = true
    private val currentMusicList = mutableListOf<MusicDownloadItem>()

    // 분석 취소 플래그
    @Volatile
    private var isAnalysisCancelled = false

    companion object {
        private const val PAGE_SIZE = 20

        fun newInstance(): MusicDownloadFragment {
            return MusicDownloadFragment()
        }
    }

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
        setupSearch()
        setupFilters()
        setupTabs()
        observeDownloadProgress()
        checkStoragePermission()

        loadMusicFromApi()
    }

    private fun setupAdapters() {
        downloadAdapter = MusicDownloadAdapter { musicItem ->
            startDownload(musicItem)
        }

        downloadedAdapter = DownloadedFileAdapter(
            onItemClick = { file ->
                // 다운로드 중인지 확인
                if (downloadManager.isDownloading.value) {
                    ToastManager.showToast("다운로드 중입니다. 완료 후 시도해주세요.")
                } else {
                    showDownloadedMusicActionDialog(file)
                }
            },
            onDeleteClick = { file ->
                // 다운로드 중인지 확인
                if (downloadManager.isDownloading.value) {
                    ToastManager.showToast("다운로드 중입니다. 완료 후 시도해주세요.")
                } else {
                    deleteFile(file)
                }
            }
        )

        binding.rvDownloadList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadAdapter
            addOnScrollListener(paginationScrollListener)
        }

        binding.rvDownloadedList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadedAdapter
        }
    }

    // 무한 스크롤을 위한 ScrollListener
    private val paginationScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val totalItemCount = layoutManager.itemCount
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

            // 마지막 아이템이 보이면 다음 페이지 로드
            if (!isLoading && !isLoadingMore && hasMoreData && totalItemCount > 0) {
                if (lastVisibleItemPosition >= totalItemCount - 3) {
                    loadMoreMusic()
                }
            }
        }
    }

    private fun setupViews() {
        binding.fabRefresh.setOnClickListener {
            binding.etSearch.setText("")
            loadMusicFromApi()
        }
    }

    private fun setupSearch() {
        // 검색 버튼 클릭시
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchMusic(query)
                binding.etSearch.clearFocus()
                // 키보드 숨기기
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            } else {
                ToastManager.showToast("검색어를 입력해주세요")
            }
        }

        // 키보드의 검색 버튼 클릭시
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchMusic(query)
                    binding.etSearch.clearFocus()
                    // 키보드 숨기기
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                } else {
                    ToastManager.showToast("검색어를 입력해주세요")
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val checkedId = checkedIds[0]
            currentFilter = when (checkedId) {
                R.id.chip_featured -> "featured"
                R.id.chip_pop -> "pop"
                R.id.chip_rock -> "rock"
                R.id.chip_electronic -> "electronic"
                R.id.chip_jazz -> "jazz"
                R.id.chip_classical -> "classical"
                else -> "featured"
            }

            binding.etSearch.setText("")
            loadMusicByFilter(currentFilter)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchTab(Tab.DOWNLOAD)
                    1 -> switchTab(Tab.DOWNLOADED)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab

        when (tab) {
            Tab.DOWNLOAD -> {
                binding.rvDownloadList.isVisible = true
                binding.rvDownloadedList.isVisible = false
                binding.etSearch.setText("")
                binding.rvDownloadList.scrollToPosition(0)
                updateEmptyState(downloadAdapter.currentList.isEmpty())
            }
            Tab.DOWNLOADED -> {
                binding.rvDownloadList.isVisible = false
                binding.rvDownloadedList.isVisible = true
                binding.etSearch.setText("")
                loadDownloadedFiles()
                binding.rvDownloadedList.scrollToPosition(0)
            }
        }
    }

    private fun searchMusic(query: String) {
        if (isLoading) return
        isLoading = true

        // 새 검색 시 페이징 초기화
        resetPaging()
        currentSearchQuery = query

        viewLifecycleOwner.lifecycleScope.launch {
            progressDialog.show("검색 중...")
            try {
                val musicList = withContext(Dispatchers.IO) {
                    try {
                        val response = jamendoRepository.searchTracks(query = query, limit = PAGE_SIZE, offset = 0)
                        if (response.isSuccessful && response.body() != null) {
                            val (results, apiCount) = parseJamendoResponse(response.body()!!)
                            hasMoreData = apiCount >= PAGE_SIZE // API가 PAGE_SIZE만큼 반환하면 더 있을 수 있음
                            currentOffset = apiCount // API 원본 개수로 offset 설정
                            results
                        } else {
                            LogManager.e("Jamendo search failed: ${response.code()}")
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("Jamendo search error: ${e.message}")
                        e.printStackTrace()
                        hasMoreData = false
                        emptyList()
                    }
                }

                currentMusicList.clear()
                currentMusicList.addAll(musicList)
                downloadAdapter.submitList(currentMusicList.toList())
                updateEmptyState(musicList.isEmpty())

                if (musicList.isEmpty()) {
                    ToastManager.showToast("'$query' 검색 결과가 없습니다")
                } else {
                    ToastManager.showToast("${musicList.size}개의 검색 결과")
                }

                // RecyclerView 렌더링 완료 후 프로그레스 dismiss 및 최상단 스크롤
                binding.rvDownloadList.post {
                    progressDialog.dismiss()
                    binding.rvDownloadList.scrollToPosition(0)
                }
                isLoading = false
            } catch (e: Exception) {
                LogManager.e("Failed to search music: ${e.message}")
                ToastManager.showToast("검색 실패: ${e.message}")
                progressDialog.dismiss()
                isLoading = false
            }
        }
    }

    private fun loadMusicByFilter(filter: String) {
        if (isLoading) return
        isLoading = true

        // 필터 변경 시 페이징 초기화
        resetPaging()
        currentSearchQuery = null

        viewLifecycleOwner.lifecycleScope.launch {
            progressDialog.show("음악 로딩 중...")
            try {
                val musicList = withContext(Dispatchers.IO) {
                    try {
                        val response = if (filter == "featured") {
                            jamendoRepository.getFeaturedTracks(limit = PAGE_SIZE, offset = 0)
                        } else {
                            jamendoRepository.getTracksByTags(tags = filter, limit = PAGE_SIZE, offset = 0)
                        }

                        if (response.isSuccessful && response.body() != null) {
                            val (results, apiCount) = parseJamendoResponse(response.body()!!)
                            hasMoreData = apiCount >= PAGE_SIZE // API가 PAGE_SIZE만큼 반환하면 더 있을 수 있음
                            currentOffset = apiCount // API 원본 개수로 offset 설정
                            results
                        } else {
                            LogManager.e("Jamendo API failed: ${response.code()}")
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("Jamendo API error: ${e.message}")
                        e.printStackTrace()
                        hasMoreData = false
                        emptyList()
                    }
                }

                currentMusicList.clear()
                currentMusicList.addAll(musicList)
                downloadAdapter.submitList(currentMusicList.toList())
                updateEmptyState(musicList.isEmpty())

                // RecyclerView 렌더링 완료 후 프로그레스 dismiss 및 최상단 스크롤
                binding.rvDownloadList.post {
                    progressDialog.dismiss()
                    binding.rvDownloadList.scrollToPosition(0)
                }
                isLoading = false
            } catch (e: Exception) {
                LogManager.e("Failed to load music: ${e.message}")
                ToastManager.showToast("음악 로드 실패: ${e.message}")
                progressDialog.dismiss()
                isLoading = false
            }
        }
    }

    private fun loadMusicFromApi() {
        loadMusicByFilter(currentFilter)
    }

    private fun resetPaging() {
        currentOffset = 0
        hasMoreData = true
        currentMusicList.clear()
    }

    private fun loadMoreMusic() {
        if (isLoadingMore || !hasMoreData) return
        isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val newMusicList = withContext(Dispatchers.IO) {
                    try {
                        val response = when {
                            currentSearchQuery != null -> {
                                jamendoRepository.searchTracks(
                                    query = currentSearchQuery!!,
                                    limit = PAGE_SIZE,
                                    offset = currentOffset
                                )
                            }
                            currentFilter == "featured" -> {
                                jamendoRepository.getFeaturedTracks(
                                    limit = PAGE_SIZE,
                                    offset = currentOffset
                                )
                            }
                            else -> {
                                jamendoRepository.getTracksByTags(
                                    tags = currentFilter,
                                    limit = PAGE_SIZE,
                                    offset = currentOffset
                                )
                            }
                        }

                        if (response.isSuccessful && response.body() != null) {
                            val (results, apiCount) = parseJamendoResponse(response.body()!!)
                            // API가 PAGE_SIZE 미만 반환하면 더 이상 데이터 없음
                            if (apiCount < PAGE_SIZE) {
                                hasMoreData = false
                            }
                            currentOffset += apiCount // API 원본 개수로 offset 증가
                            results
                        } else {
                            LogManager.e("Jamendo API failed: ${response.code()}")
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("Jamendo API error: ${e.message}")
                        e.printStackTrace()
                        hasMoreData = false
                        emptyList()
                    }
                }

                if (newMusicList.isNotEmpty()) {
                    currentMusicList.addAll(newMusicList)
                    downloadAdapter.submitList(currentMusicList.toList())
                }

                isLoadingMore = false
            } catch (e: Exception) {
                LogManager.e("Failed to load more music: ${e.message}")
                isLoadingMore = false
            }
        }
    }

    // Pair<필터링된 리스트, API 원본 결과 개수>
    @SuppressLint("DefaultLocale")
    private fun parseJamendoResponse(jamendoResponse: com.example.musicplayer.server.data.JamendoResponse): Pair<List<MusicDownloadItem>, Int> {
        val apiResultCount = jamendoResponse.headers.resultsCount
        val filteredList = jamendoResponse.results
            .filter { it.audioDownloadAllowed }
            .map { track ->
                val durationMinutes = track.duration / 60
                val durationSeconds = track.duration % 60
                val durationText = String.format("%d:%02d", durationMinutes, durationSeconds)

                MusicDownloadItem(
                    id = track.id,
                    title = track.name,
                    artist = track.artistName,
                    url = track.audioDownload,
                    duration = durationText,
                    size = "MP3",
                    genre = "Jamendo",
                    description = "",
                    thumbnailUrl = track.image ?: track.albumImage ?: "",
                    imageUrl = track.image ?: track.albumImage,
                    albumName = track.albumName
                )
            }
        return Pair(filteredList, apiResultCount)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (currentTab == Tab.DOWNLOAD) {
            binding.layoutEmpty.isVisible = isEmpty
            val searchQuery = binding.etSearch.text.toString().trim()
            binding.tvEmptyMessage.text = if (searchQuery.isEmpty()) {
                "다운로드 가능한 음악이 없습니다"
            } else {
                "검색 결과가 없습니다"
            }
        } else {
            binding.layoutEmpty.isVisible = isEmpty
            binding.tvEmptyMessage.text = "다운로드된 파일이 없습니다"
        }
    }

    private fun loadDownloadedFiles() {
        val downloadedFiles = downloadManager.getDownloadedFiles(requireContext())
        downloadedAdapter.submitList(downloadedFiles)
        updateEmptyState(downloadedFiles.isEmpty())
    }

    private fun startDownload(musicItem: MusicDownloadItem) {
        if (musicItem.url.isEmpty()) {
            ToastManager.showToast("다운로드 URL이 없습니다")
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

        ToastManager.showToast("다운로드 시작: ${musicItem.title}")
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
                loadDownloadedFiles()
            } else if (progress.isError) {
                ToastManager.showToast("다운로드 실패: ${progress.errorMessage}")
                layoutDownloadProgress.isVisible = false
            }
        }

        binding.btnCancelDownload.setOnClickListener {
            currentDownloadId?.let { downloadManager.cancelDownload(it) }
            binding.layoutDownloadProgress.isVisible = false
        }
    }

    /**
     * 다운로드된 음악 액션 선택 다이얼로그 표시
     */
    @SuppressLint("InflateParams")
    private fun showDownloadedMusicActionDialog(file: File) {
        // 파일에서 MusicFile 객체 생성
        val retriever = MediaMetadataRetriever()
        val musicFile: MusicFile
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension.replace("_", " ")
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            retriever.release()

            musicFile = MusicFile(
                uri = Uri.fromFile(file),
                title = title,
                artist = artist,
                duration = duration
            )
        } catch (_: Exception) {
            retriever.release()
            ToastManager.showToast("파일 정보를 읽을 수 없습니다")
            return
        }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_music_action, null)
        )
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 음악 제목 설정
        dialog.findViewById<TextView>(R.id.tv_music_title).text = musicFile.title

        // 녹음하기 버튼
        dialog.findViewById<LinearLayout>(R.id.btn_go_recording).setOnClickListener {
            dialog.dismiss()
            // 음악 분석 후 녹음 페이지로 이동
            analyzeAndNavigateToRecording(musicFile)
        }

        // 음악 듣기 버튼
        dialog.findViewById<LinearLayout>(R.id.btn_play_music).setOnClickListener {
            dialog.dismiss()
            // 음악 재생 페이지로 이동
            playAudioFile(file)
        }

        // 취소 버튼
        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 너비 설정
        dialog.window?.let { window ->
            val displayMetrics = requireContext().resources.displayMetrics
            val marginDp = 10f
            val marginPx = (marginDp * displayMetrics.density).toInt()
            val params = window.attributes
            params.width = displayMetrics.widthPixels - (marginPx * 2)
            window.attributes = params
        }
    }

    /**
     * 음악 분석 후 녹음 페이지로 이동
     */
    private fun analyzeAndNavigateToRecording(music: MusicFile) {
        isAnalysisCancelled = false

        viewLifecycleOwner.lifecycleScope.launch {
            progressDialog.showCancelable("0% 분석 중...") {
                isAnalysisCancelled = true
                ToastManager.showToast("분석이 취소되었습니다")
            }

            try {
                val pitchList = withContext(Dispatchers.Default) {
                    MusicFileDispatcherFactory.analyzePitchFromMediaUri(
                        context = requireContext(),
                        uri = music.uri,
                        onProgress = { progress ->
                            if (!isAnalysisCancelled) {
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                                    progressDialog.updateMessage("$progress% 분석 중...")
                                }
                            }
                        }
                    )
                }

                if (isAnalysisCancelled) {
                    LogManager.i("MusicDownloadFragment: Analysis was cancelled")
                    return@launch
                }

                progressDialog.dismiss()

                // RecordingFragment로 네비게이션
                val bundle = Bundle().apply {
                    putParcelable("music", music)
                    putFloatArray("pitchArray", pitchList.toFloatArray())
                    putLong("durationMillis", music.duration)
                }
                findNavController().navigate(R.id.action_search_to_recording, bundle)

            } catch (e: Exception) {
                progressDialog.dismiss()
                if (!isAnalysisCancelled) {
                    LogManager.e("Failed to analyze music: ${e.message}")
                    ToastManager.showToast("분석 실패: ${e.message}")
                }
            }
        }
    }

    private fun playAudioFile(file: File) {
        try {
            // 다운로드된 모든 파일 리스트에서 MusicFile 리스트 생성
            val downloadedFiles = downloadManager.getDownloadedFiles(requireContext())
            val musicList = ArrayList<MusicFile>()
            var currentIndex = 0

            downloadedFiles.forEachIndexed { index, downloadedFile ->
                val fileRetriever = MediaMetadataRetriever()
                try {
                    fileRetriever.setDataSource(downloadedFile.absolutePath)
                    val fileDuration = fileRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val fileTitle = fileRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: downloadedFile.nameWithoutExtension.replace("_", " ")
                    val fileArtist = fileRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    fileRetriever.release()

                    musicList.add(MusicFile(
                        uri = Uri.fromFile(downloadedFile),
                        title = fileTitle,
                        artist = fileArtist,
                        duration = fileDuration
                    ))

                    // 현재 재생하려는 파일의 인덱스 찾기
                    if (downloadedFile.absolutePath == file.absolutePath) {
                        currentIndex = index
                    }
                } catch (_: Exception) {
                    fileRetriever.release()
                }
            }

            // MusicPlayerFragment로 네비게이션
            val bundle = Bundle().apply {
                putParcelableArray("musicList", musicList.toTypedArray())
                putInt("currentIndex", currentIndex)
            }

            findNavController().navigate(R.id.action_search_to_musicPlayer, bundle)

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
        binding.rvDownloadList.removeOnScrollListener(paginationScrollListener)
        downloadManager.release()
        if (progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
        _binding = null
    }
}
