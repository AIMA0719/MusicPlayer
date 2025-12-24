package com.example.musicplayer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.DownloadedFileAdapter
import com.example.musicplayer.adapter.MusicDownloadAdapter
import com.example.musicplayer.adapter.MusicListAdapter
import com.example.musicplayer.data.MusicDownloadItem
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.MusicListIntent
import com.example.musicplayer.data.SingingMode
import com.example.musicplayer.databinding.FragmentKaraokeBinding
import com.example.musicplayer.factory.MusicFileDispatcherFactory
import com.example.musicplayer.factory.MusicListViewModelFactory
import com.example.musicplayer.manager.FileDownloadManager
import com.example.musicplayer.manager.IOSStyleProgressDialog
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.JamendoRepository
import com.example.musicplayer.viewModel.MusicListViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 노래방 탭 - 온라인/내 음악/다운로드된 곡 통합 관리
 */
@AndroidEntryPoint
class KaraokeFragment : Fragment() {

    private var _binding: FragmentKaraokeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var jamendoRepository: JamendoRepository

    // 뷰모델 - 로컬 음악용
    private val musicListViewModel: MusicListViewModel by lazy {
        val application = requireActivity().application
        val factory = MusicListViewModelFactory(application)
        ViewModelProvider(this, factory)[MusicListViewModel::class.java]
    }

    // 어댑터들
    private lateinit var onlineAdapter: MusicDownloadAdapter
    private lateinit var localAdapter: MusicListAdapter
    private lateinit var downloadedAdapter: DownloadedFileAdapter

    // 다운로드 관리
    private lateinit var downloadManager: FileDownloadManager
    private lateinit var progressDialog: IOSStyleProgressDialog

    private var currentDownloadId: String? = null
    private var isLoading = false
    private var isLoadingMore = false
    private var currentTab = Tab.ONLINE
    private var currentFilter = "featured"
    private var currentSearchQuery: String? = null

    // 페이징
    private var currentOffset = 0
    private var hasMoreData = true
    private val currentMusicList = mutableListOf<MusicDownloadItem>()

    // 분석 취소
    @Volatile
    private var isAnalysisCancelled = false

    // 선택된 노래 모드 (연습/도전)
    private var selectedSingingMode: SingingMode = SingingMode.PRACTICE

    private enum class Tab {
        ONLINE, LOCAL, DOWNLOADED
    }

    companion object {
        private const val PAGE_SIZE = 20

        fun newInstance(): KaraokeFragment {
            return KaraokeFragment()
        }
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
        _binding = FragmentKaraokeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadManager = FileDownloadManager()
        progressDialog = IOSStyleProgressDialog(requireContext())

        setupAdapters()
        setupTabs()
        setupSearch()
        setupFilters()
        setupRefresh()
        observeDownloadProgress()
        observeLocalMusicList()
        checkStoragePermission()

        // 초기 로드
        loadOnlineMusic()
    }

    private fun setupAdapters() {
        // 온라인 음악 어댑터
        onlineAdapter = MusicDownloadAdapter { musicItem ->
            showSongActionBottomSheet(musicItem)
        }

        // 로컬 음악 어댑터
        localAdapter = MusicListAdapter { musicFile ->
            showLocalMusicActionDialog(musicFile)
        }

        // 다운로드된 파일 어댑터
        downloadedAdapter = DownloadedFileAdapter(
            onItemClick = { file ->
                if (downloadManager.isDownloading.value) {
                    ToastManager.showToast("다운로드 중입니다. 완료 후 시도해주세요.")
                } else {
                    showDownloadedMusicActionDialog(file)
                }
            },
            onDeleteClick = { file ->
                if (downloadManager.isDownloading.value) {
                    ToastManager.showToast("다운로드 중입니다. 완료 후 시도해주세요.")
                } else {
                    deleteFile(file)
                }
            }
        )

        binding.rvMusicList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = onlineAdapter
            addOnScrollListener(paginationScrollListener)
        }
    }

    private val paginationScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (currentTab != Tab.ONLINE) return

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val totalItemCount = layoutManager.itemCount
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

            if (!isLoading && !isLoadingMore && hasMoreData && totalItemCount > 0) {
                if (lastVisibleItemPosition >= totalItemCount - 3) {
                    loadMoreOnlineMusic()
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("온라인"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("내 음악"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("다운로드"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchTab(Tab.ONLINE)
                    1 -> switchTab(Tab.LOCAL)
                    2 -> switchTab(Tab.DOWNLOADED)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        binding.etSearch.setText("")

        when (tab) {
            Tab.ONLINE -> {
                binding.chipScrollView.isVisible = true
                binding.rvMusicList.adapter = onlineAdapter
                updateEmptyState(onlineAdapter.currentList.isEmpty())
                if (onlineAdapter.currentList.isEmpty()) {
                    loadOnlineMusic()
                }
            }
            Tab.LOCAL -> {
                binding.chipScrollView.isVisible = false
                binding.rvMusicList.adapter = localAdapter
                musicListViewModel.onIntent(MusicListIntent.LoadMusicFiles)
            }
            Tab.DOWNLOADED -> {
                binding.chipScrollView.isVisible = false
                binding.rvMusicList.adapter = downloadedAdapter
                loadDownloadedFiles()
            }
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isEmpty()) {
            ToastManager.showToast("검색어를 입력해주세요")
            return
        }

        hideKeyboard()

        when (currentTab) {
            Tab.ONLINE -> searchOnlineMusic(query)
            Tab.LOCAL -> {
                // 로컬 음악 필터링은 현재 지원하지 않음
                ToastManager.showToast("로컬 음악에서는 검색이 지원되지 않습니다")
            }
            Tab.DOWNLOADED -> {
                // 다운로드된 파일 필터링
                ToastManager.showToast("다운로드된 파일에서는 검색이 지원되지 않습니다")
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty() || currentTab != Tab.ONLINE) return@setOnCheckedStateChangeListener

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
            loadOnlineByFilter(currentFilter)
        }
    }

    private fun setupRefresh() {
        binding.fabRefresh.setOnClickListener {
            binding.etSearch.setText("")
            when (currentTab) {
                Tab.ONLINE -> loadOnlineMusic()
                Tab.LOCAL -> musicListViewModel.onIntent(MusicListIntent.LoadMusicFiles)
                Tab.DOWNLOADED -> loadDownloadedFiles()
            }
        }
    }

    // ===================== 온라인 음악 로드 =====================

    private fun loadOnlineMusic() {
        loadOnlineByFilter(currentFilter)
    }

    private fun searchOnlineMusic(query: String) {
        if (isLoading) return
        isLoading = true

        resetPaging()
        currentSearchQuery = query

        // 로딩 상태 표시
        showLoading(true, "검색 중...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val musicList = withContext(Dispatchers.IO) {
                    try {
                        val response = jamendoRepository.searchTracks(query = query, limit = PAGE_SIZE, offset = 0)
                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            val (results, apiCount) = parseJamendoResponse(body)
                            hasMoreData = apiCount >= PAGE_SIZE
                            currentOffset = apiCount
                            results
                        } else {
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("Jamendo search error: ${e.message}")
                        hasMoreData = false
                        emptyList()
                    }
                }

                currentMusicList.clear()
                currentMusicList.addAll(musicList)
                onlineAdapter.submitList(currentMusicList.toList())
                updateEmptyState(musicList.isEmpty())

                if (musicList.isEmpty()) {
                    ToastManager.showToast("'$query' 검색 결과가 없습니다")
                } else {
                    ToastManager.showToast("${musicList.size}개의 검색 결과")
                }

                _binding?.rvMusicList?.post {
                    showLoading(false)
                    _binding?.rvMusicList?.scrollToPosition(0)
                }
                isLoading = false
            } catch (e: CancellationException) {
                // 코루틴 취소는 정상적인 동작이므로 다시 던짐
                throw e
            } catch (e: Exception) {
                LogManager.e("Failed to search music: ${e.message}")
                ToastManager.showToast("검색 실패: ${e.message}")
                showLoading(false)
                isLoading = false
            }
        }
    }

    private fun loadOnlineByFilter(filter: String) {
        if (isLoading) return
        isLoading = true

        resetPaging()
        currentSearchQuery = null

        // 로딩 상태 표시
        showLoading(true, "음악 로딩 중...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val musicList = withContext(Dispatchers.IO) {
                    try {
                        val response = if (filter == "featured") {
                            jamendoRepository.getFeaturedTracks(limit = PAGE_SIZE, offset = 0)
                        } else {
                            jamendoRepository.getTracksByTags(tags = filter, limit = PAGE_SIZE, offset = 0)
                        }

                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            val (results, apiCount) = parseJamendoResponse(body)
                            hasMoreData = apiCount >= PAGE_SIZE
                            currentOffset = apiCount
                            results
                        } else {
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (e: Exception) {
                        LogManager.e("Jamendo API error: ${e.message}")
                        hasMoreData = false
                        emptyList()
                    }
                }

                currentMusicList.clear()
                currentMusicList.addAll(musicList)
                onlineAdapter.submitList(currentMusicList.toList())
                updateEmptyState(musicList.isEmpty())

                _binding?.rvMusicList?.post {
                    showLoading(false)
                    _binding?.rvMusicList?.scrollToPosition(0)
                }
                isLoading = false
            } catch (e: CancellationException) {
                // 코루틴 취소는 정상적인 동작이므로 다시 던짐
                throw e
            } catch (e: Exception) {
                LogManager.e("Failed to load music: ${e.message}")
                ToastManager.showToast("음악 로드 실패: ${e.message}")
                showLoading(false)
                isLoading = false
            }
        }
    }

    private fun showLoading(show: Boolean, message: String = "로딩 중...") {
        if (_binding == null) return
        binding.layoutLoading.isVisible = show
        binding.rvMusicList.isVisible = !show && !binding.layoutEmpty.isVisible
        if (show) {
            binding.tvLoadingMessage.text = message
            binding.layoutEmpty.isVisible = false
        }
    }

    private fun loadMoreOnlineMusic() {
        if (isLoadingMore || !hasMoreData) return
        isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val newMusicList = withContext(Dispatchers.IO) {
                    try {
                        val searchQuery = currentSearchQuery
                        val response = when {
                            searchQuery != null -> {
                                jamendoRepository.searchTracks(
                                    query = searchQuery,
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

                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            val (results, apiCount) = parseJamendoResponse(body)
                            if (apiCount < PAGE_SIZE) {
                                hasMoreData = false
                            }
                            currentOffset += apiCount
                            results
                        } else {
                            hasMoreData = false
                            emptyList()
                        }
                    } catch (_: Exception) {
                        hasMoreData = false
                        emptyList()
                    }
                }

                if (newMusicList.isNotEmpty()) {
                    currentMusicList.addAll(newMusicList)
                    onlineAdapter.submitList(currentMusicList.toList())
                }

                isLoadingMore = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isLoadingMore = false
            }
        }
    }

    private fun resetPaging() {
        currentOffset = 0
        hasMoreData = true
        currentMusicList.clear()
    }

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

    // ===================== 로컬 음악 =====================

    private fun observeLocalMusicList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicListViewModel.container.stateFlow.collect { state ->
                    if (currentTab == Tab.LOCAL) {
                        localAdapter.submitList(state.musicFiles)
                        updateEmptyState(state.musicFiles.isEmpty())
                    }

                    // 분석 진행 상태
                    if (state.isAnalyzing) {
                        if (!progressDialog.isShowing()) {
                            progressDialog.showCancelable("${state.analysisProgress}% 분석 중...") {
                                musicListViewModel.onIntent(MusicListIntent.CancelAnalysis)
                                ToastManager.showToast("분석이 취소되었습니다")
                            }
                        } else {
                            progressDialog.updateMessage("${state.analysisProgress}% 분석 중...")
                        }
                    } else {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss()
                        }
                    }

                    // 분석 완료 후 녹음 화면으로 이동
                    if (state.originalPitch != null && state.selectedMusic != null && !state.isAnalyzing && !state.hasNavigated) {
                        try {
                            val bundle = Bundle().apply {
                                putParcelable("music", state.selectedMusic)
                                putFloatArray("pitchArray", state.originalPitch.toFloatArray())
                                putLong("durationMillis", state.selectedMusic.duration)
                                putString("singingMode", selectedSingingMode.name)
                            }
                            findNavController().navigate(R.id.action_karaoke_to_recording, bundle)
                            musicListViewModel.onIntent(MusicListIntent.MarkAsNavigated)
                        } catch (e: Exception) {
                            LogManager.e("Navigation failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun showLocalMusicActionDialog(music: MusicFile) {
        val bottomSheet = SongDetailBottomSheet.newInstance(
            title = music.title,
            artist = music.artist,
            duration = music.duration,
            isOnline = false
        )
        bottomSheet.setOnActionListener(object : SongDetailBottomSheet.OnActionListener {
            override fun onPracticeMode() {
                selectedSingingMode = SingingMode.PRACTICE
                musicListViewModel.onIntent(MusicListIntent.AnalyzeOriginalMusic(music))
            }

            override fun onChallengeMode() {
                selectedSingingMode = SingingMode.CHALLENGE
                musicListViewModel.onIntent(MusicListIntent.AnalyzeOriginalMusic(music))
            }

            override fun onDownload() {
                // 로컬 파일은 다운로드 불필요
            }
        })
        bottomSheet.show(childFragmentManager, "SongDetailBottomSheet")
    }

    // ===================== 다운로드된 파일 =====================

    private fun loadDownloadedFiles() {
        val downloadedFiles = downloadManager.getDownloadedFiles(requireContext())
        downloadedAdapter.submitList(downloadedFiles)
        updateEmptyState(downloadedFiles.isEmpty())
    }

    private fun showDownloadedMusicActionDialog(file: File) {
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

        val bottomSheet = SongDetailBottomSheet.newInstance(
            title = musicFile.title,
            artist = musicFile.artist,
            duration = musicFile.duration,
            isOnline = false
        )
        bottomSheet.setOnActionListener(object : SongDetailBottomSheet.OnActionListener {
            override fun onPracticeMode() {
                selectedSingingMode = SingingMode.PRACTICE
                analyzeAndNavigateToRecording(musicFile)
            }

            override fun onChallengeMode() {
                selectedSingingMode = SingingMode.CHALLENGE
                analyzeAndNavigateToRecording(musicFile)
            }

            override fun onDownload() {}
        })
        bottomSheet.show(childFragmentManager, "SongDetailBottomSheet")
    }

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
                    return@launch
                }

                progressDialog.dismiss()

                val bundle = Bundle().apply {
                    putParcelable("music", music)
                    putFloatArray("pitchArray", pitchList.toFloatArray())
                    putLong("durationMillis", music.duration)
                    putString("singingMode", selectedSingingMode.name)
                }
                findNavController().navigate(R.id.action_karaoke_to_recording, bundle)

            } catch (e: CancellationException) {
                progressDialog.dismiss()
                throw e
            } catch (e: Exception) {
                progressDialog.dismiss()
                if (!isAnalysisCancelled) {
                    LogManager.e("Failed to analyze music: ${e.message}")
                    ToastManager.showToast("분석 실패: ${e.message}")
                }
            }
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
            ToastManager.showToast("파일 삭제 오류: ${e.message}")
        }
    }

    // ===================== 온라인 음악 액션 =====================

    private fun showSongActionBottomSheet(musicItem: MusicDownloadItem) {
        val durationParts = musicItem.duration.split(":")
        val durationMs = if (durationParts.size == 2) {
            (durationParts[0].toIntOrNull() ?: 0) * 60000L + (durationParts[1].toIntOrNull() ?: 0) * 1000L
        } else {
            0L
        }

        val bottomSheet = SongDetailBottomSheet.newInstance(
            title = musicItem.title,
            artist = musicItem.artist,
            duration = durationMs,
            isOnline = true,
            thumbnailUrl = musicItem.thumbnailUrl
        )
        bottomSheet.setOnActionListener(object : SongDetailBottomSheet.OnActionListener {
            override fun onPracticeMode() {
                // 먼저 다운로드 필요
                startDownloadAndAnalyze(musicItem)
            }

            override fun onChallengeMode() {
                startDownloadAndAnalyze(musicItem)
            }

            override fun onDownload() {
                startDownload(musicItem)
            }
        })
        bottomSheet.show(childFragmentManager, "SongDetailBottomSheet")
    }

    private fun startDownload(musicItem: MusicDownloadItem) {
        if (musicItem.url.isEmpty()) {
            ToastManager.showToast("다운로드 URL이 없습니다")
            return
        }

        // 이미 다운로드 중인지 확인
        if (downloadManager.isDownloading.value) {
            ToastManager.showToast("다른 다운로드가 진행 중입니다. 완료 후 시도해주세요.")
            return
        }

        val fileName = "${musicItem.title.replace(" ", "_")}_${musicItem.artist.replace(" ", "_")}.mp3"

        // 이미 다운로드된 파일인지 확인
        val downloadDir = File(requireContext().getExternalFilesDir(null), "Downloads")
        val existingFile = File(downloadDir, fileName)
        if (existingFile.exists()) {
            ToastManager.showToast("이미 다운로드된 파일입니다")
            return
        }

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

    private fun startDownloadAndAnalyze(musicItem: MusicDownloadItem) {
        // 이미 다운로드 중인지 확인
        if (downloadManager.isDownloading.value) {
            ToastManager.showToast("다운로드 완료 후 시도해주세요")
            return
        }

        // 이미 다운로드된 파일인지 확인
        val fileName = "${musicItem.title.replace(" ", "_")}_${musicItem.artist.replace(" ", "_")}.mp3"
        val downloadDir = File(requireContext().getExternalFilesDir(null), "Downloads")
        val existingFile = File(downloadDir, fileName)

        if (existingFile.exists()) {
            // 이미 다운로드됨 - 바로 분석
            val musicFile = createMusicFileFromDownload(existingFile)
            if (musicFile != null) {
                analyzeAndNavigateToRecording(musicFile)
            } else {
                ToastManager.showToast("파일을 읽을 수 없습니다")
            }
        } else {
            // 다운로드 필요
            ToastManager.showToast("먼저 다운로드를 완료해주세요")
            startDownload(musicItem)
        }
    }

    private fun createMusicFileFromDownload(file: File): MusicFile? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension.replace("_", " ")
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            retriever.release()

            MusicFile(
                uri = Uri.fromFile(file),
                title = title,
                artist = artist,
                duration = duration
            )
        } catch (_: Exception) {
            retriever.release()
            null
        }
    }

    private fun observeDownloadProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadManager.downloadProgress.collectLatest { progressMap ->
                    if (_binding == null) return@collectLatest
                    // 처리되지 않은 다운로드만 필터링
                    val activeDownloads = progressMap.values.filter { !it.hasBeenHandled }
                    if (activeDownloads.isNotEmpty()) {
                        val progress = activeDownloads.first()
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
        if (_binding == null) return
        binding.apply {
            // 이미 처리된 완료/에러는 무시
            if (progress.hasBeenHandled) {
                layoutDownloadProgress.isVisible = false
                return
            }

            progressDownload.progress = progress.progress
            tvDownloadProgress.text = "${progress.progress}%"

            val downloadedSize = FileDownloadManager.formatFileSize(progress.downloadedBytes)
            val totalSize = FileDownloadManager.formatFileSize(progress.totalBytes)
            tvDownloadSize.text = "$downloadedSize / $totalSize"

            if (progress.isCompleted) {
                // 토스트 표시 후 처리 완료 표시
                ToastManager.showToast("다운로드가 완료되었습니다!")
                layoutDownloadProgress.isVisible = false
                downloadManager.markAsHandled(progress.downloadId)
                // 완료된 다운로드 제거
                downloadManager.removeDownload(progress.downloadId)
                if (currentTab == Tab.DOWNLOADED) {
                    loadDownloadedFiles()
                }
            } else if (progress.isError) {
                ToastManager.showToast("다운로드 실패: ${progress.errorMessage}")
                layoutDownloadProgress.isVisible = false
                downloadManager.markAsHandled(progress.downloadId)
                downloadManager.removeDownload(progress.downloadId)
            } else {
                // 다운로드 진행 중
                layoutDownloadProgress.isVisible = true
            }
        }

        binding.btnCancelDownload.setOnClickListener {
            currentDownloadId?.let { downloadManager.cancelDownload(it) }
            binding.layoutDownloadProgress.isVisible = false
        }
    }

    // ===================== 유틸리티 =====================

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvMusicList.isVisible = !isEmpty && !binding.layoutLoading.isVisible
        binding.tvEmptyMessage.text = when (currentTab) {
            Tab.ONLINE -> {
                val searchQuery = binding.etSearch.text.toString().trim()
                if (searchQuery.isEmpty()) "노래를 검색해보세요" else "검색 결과가 없습니다"
            }
            Tab.LOCAL -> "기기에 저장된 음악이 없습니다"
            Tab.DOWNLOADED -> "다운로드된 파일이 없습니다"
        }
    }

    private fun checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == Tab.LOCAL) {
            musicListViewModel.onIntent(MusicListIntent.LoadMusicFiles)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMusicList.removeOnScrollListener(paginationScrollListener)
        downloadManager.release()
        if (progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
        _binding = null
    }
}
