package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.data.SingingMode
import com.example.musicplayer.databinding.FragmentRecordingBinding
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.manager.GameManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.PitchShiftManager
import com.example.musicplayer.manager.ScoreFeedbackDialogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.viewModel.ScoreViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordingFragment : Fragment() {

    private val scoreViewModel: ScoreViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val viewModel: RecordingViewModel by viewModels()

    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray
    private var durationMillis: Long = 0
    private var singingMode: SingingMode = SingingMode.PRACTICE  // 기본값: 연습 모드

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    // 가이드 음성 재생용 MediaPlayer
    private var guidePlayer: MediaPlayer? = null
    private var isGuidePlayerPrepared = false

    private var wasRecording = false
    private var lastUserX = -1f
    private var lastOriginIndex = -1

    // 선택된 난이도 저장
    private var selectedDifficulty: ScoreFeedbackDialogManager.ScoringDifficulty? = null

    // 피드백 다이얼로그 표시 여부 (중복 방지)
    private var hasFeedbackShown = false

    // 게임 매니저
    private lateinit var gameManager: GameManager
    private var gameManagerInitJob: Job? = null

    // 키 변경 (반음 단위: -6 ~ +6)
    private var currentPitchSemitones: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("music", MusicFile::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("music")!!
            }
            pitchArray = it.getFloatArray("pitchArray")!!
            durationMillis = it.getLong("durationMillis", 0L)

            // SingingMode 받기 (기본값: PRACTICE)
            val modeName = it.getString("singingMode", SingingMode.PRACTICE.name)
            singingMode = try {
                SingingMode.valueOf(modeName)
            } catch (e: Exception) {
                SingingMode.PRACTICE
            }
        }

        // GameManager 초기화 - Job 저장하여 나중에 완료 대기
        val userId = com.example.musicplayer.manager.AuthManager.getCurrentUserId() ?: "guest"
        gameManager = GameManager(requireContext(), userId)
        gameManagerInitJob = lifecycleScope.launch {
            gameManager.initialize()
        }

        // 가이드 플레이어 초기화
        initGuidePlayer()
    }

    /**
     * 가이드 음성 재생용 MediaPlayer 초기화
     */
    private fun initGuidePlayer() {
        try {
            guidePlayer = MediaPlayer().apply {
                setDataSource(requireContext(), music.uri)
                setOnPreparedListener {
                    isGuidePlayerPrepared = true
                    LogManager.d("Guide player prepared for: ${music.title}")
                }
                setOnCompletionListener {
                    LogManager.d("Guide audio completed")
                }
                setOnErrorListener { _, what, extra ->
                    LogManager.e("Guide player error: what=$what, extra=$extra")
                    isGuidePlayerPrepared = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            LogManager.e("Failed to initialize guide player: ${e.message}")
            isGuidePlayerPrepared = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        initPitchChart()
        setupViews()
        setObserver()

        return binding.root
    }

    private fun setupViews() {
        // 노래 제목 설정
        binding.songTitle.text = music.title

        // 모드 표시 설정
        setupModeIndicator()

        // 키 조절 설정
        setupKeyControl()

        // 시작 버튼 - 난이도 선택 후 녹음 시작
        binding.btnStart.setOnClickListener {
            showDifficultySelectAndStartRecording()
        }

        // 일시정지/재개 버튼
        binding.btnPause.setOnClickListener {
            if (viewModel.container.stateFlow.value.isPaused) {
                resumeRecordingWithGuide()
            } else {
                pauseRecordingWithGuide()
            }
        }

        // 정지 버튼
        binding.btnStop.setOnClickListener {
            stopRecordingWithGuide()
        }
    }

    /**
     * 모드 표시 UI 설정
     */
    @SuppressLint("SetTextI18n")
    private fun setupModeIndicator() {
        // 모드 텍스트뷰가 있으면 설정 (없으면 recordingStatus 활용)
        val modeText = when (singingMode) {
            SingingMode.PRACTICE -> "🎵 연습 모드 (가이드 ON)"
            SingingMode.CHALLENGE -> "🏆 도전 모드 (가이드 OFF)"
        }

        // recordingStatus 초기 텍스트에 모드 표시
        binding.recordingStatus.text = modeText
    }

    /**
     * 가이드 음성과 함께 일시정지
     */
    private fun pauseRecordingWithGuide() {
        viewModel.pauseRecording()
        pauseGuideAudio()
    }

    /**
     * 가이드 음성과 함께 재개
     */
    private fun resumeRecordingWithGuide() {
        viewModel.resumeRecording()
        resumeGuideAudio()
    }

    /**
     * 가이드 음성과 함께 정지
     */
    private fun stopRecordingWithGuide() {
        viewModel.stopRecording()
        stopGuideAudio()
    }

    /**
     * 가이드 오디오 시작
     */
    private fun startGuideAudio() {
        if (!singingMode.isGuideEnabled) {
            LogManager.d("Guide audio disabled in ${singingMode.displayName}")
            return
        }

        if (!isGuidePlayerPrepared) {
            LogManager.w("Guide player not prepared yet")
            return
        }

        try {
            guidePlayer?.let { player ->
                player.seekTo(0)

                // 피치 시프트 적용 (API 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentPitchSemitones != 0) {
                    val pitchRatio = PitchShiftManager.semitonesToPitchRatio(currentPitchSemitones)
                    val params = player.playbackParams
                    params.pitch = pitchRatio
                    params.speed = 1.0f  // 속도는 유지
                    player.playbackParams = params
                    LogManager.d("Guide audio pitch shifted: $pitchRatio (${currentPitchSemitones} semitones)")
                }

                player.start()
                LogManager.d("Guide audio started")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to start guide audio: ${e.message}")
        }
    }

    /**
     * 가이드 오디오 일시정지
     */
    private fun pauseGuideAudio() {
        try {
            guidePlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    LogManager.d("Guide audio paused")
                }
            }
        } catch (e: Exception) {
            LogManager.e("Failed to pause guide audio: ${e.message}")
        }
    }

    /**
     * 가이드 오디오 재개
     */
    private fun resumeGuideAudio() {
        if (!singingMode.isGuideEnabled) return

        try {
            guidePlayer?.let { player ->
                player.start()
                LogManager.d("Guide audio resumed")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to resume guide audio: ${e.message}")
        }
    }

    /**
     * 가이드 오디오 정지
     */
    private fun stopGuideAudio() {
        try {
            guidePlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                // 다시 재생할 수 있도록 prepare
                player.prepare()
                player.seekTo(0)
                LogManager.d("Guide audio stopped and reset")
            }
        } catch (e: Exception) {
            LogManager.e("Failed to stop guide audio: ${e.message}")
        }
    }

    /**
     * 가이드 플레이어 해제
     */
    private fun releaseGuidePlayer() {
        try {
            guidePlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            guidePlayer = null
            isGuidePlayerPrepared = false
            LogManager.d("Guide player released")
        } catch (e: Exception) {
            LogManager.e("Failed to release guide player: ${e.message}")
        }
    }

    /**
     * 키 조절 컨트롤 설정
     */
    private fun setupKeyControl() {
        updateKeyDisplay()

        // 키 내리기
        binding.btnKeyDown.setOnClickListener {
            if (currentPitchSemitones > PitchShiftManager.MIN_PITCH_SEMITONES) {
                currentPitchSemitones--
                updateKeyDisplay()
                applyPitchToOriginal()
            }
        }

        // 키 올리기
        binding.btnKeyUp.setOnClickListener {
            if (currentPitchSemitones < PitchShiftManager.MAX_PITCH_SEMITONES) {
                currentPitchSemitones++
                updateKeyDisplay()
                applyPitchToOriginal()
            }
        }

        // 원키 리셋
        binding.btnKeyReset.setOnClickListener {
            currentPitchSemitones = 0
            updateKeyDisplay()
            applyPitchToOriginal()
        }
    }

    /**
     * 키 표시 업데이트
     */
    @SuppressLint("SetTextI18n")
    private fun updateKeyDisplay() {
        binding.tvCurrentKey.text = PitchShiftManager.semitonesToKeyString(currentPitchSemitones)

        // 리셋 버튼 표시 여부
        binding.btnKeyReset.visibility = if (currentPitchSemitones != 0) View.VISIBLE else View.GONE

        // 버튼 활성화/비활성화
        binding.btnKeyDown.isEnabled = currentPitchSemitones > PitchShiftManager.MIN_PITCH_SEMITONES
        binding.btnKeyUp.isEnabled = currentPitchSemitones < PitchShiftManager.MAX_PITCH_SEMITONES

        binding.btnKeyDown.alpha = if (binding.btnKeyDown.isEnabled) 1.0f else 0.3f
        binding.btnKeyUp.alpha = if (binding.btnKeyUp.isEnabled) 1.0f else 0.3f
    }

    /**
     * 키 변경을 가이드 오디오에 적용
     * - 녹음 시작 시 변경된 키가 적용됨
     * - 재생 중일 때도 실시간 반영
     */
    private fun applyPitchToOriginal() {
        // 토스트 메시지
        val keyString = PitchShiftManager.semitonesToKeyString(currentPitchSemitones)
        ToastManager.showToast("키 ${keyString}로 변경됨")

        // 현재 재생 중인 가이드 오디오가 있으면 피치 업데이트
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isGuidePlayerPrepared) {
            try {
                guidePlayer?.let { player ->
                    val pitchRatio = PitchShiftManager.semitonesToPitchRatio(currentPitchSemitones)
                    val params = player.playbackParams
                    params.pitch = pitchRatio
                    params.speed = 1.0f  // 속도는 유지
                    player.playbackParams = params
                    LogManager.d("Guide audio pitch updated: $pitchRatio")
                }
            } catch (e: Exception) {
                LogManager.e("Failed to update guide audio pitch: ${e.message}")
            }
        }
    }

    /**
     * 설정된 난이도로 카운트다운 & 녹음 시작
     * 난이도 변경은 설정 화면에서만 가능
     */
    private fun showDifficultySelectAndStartRecording() {
        // SharedPreferences에서 기본 난이도 확인 (기본값: NORMAL = 2)
        val sharedPrefs = requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val defaultDifficultyIndex = sharedPrefs.getInt("default_difficulty", 2)

        // 설정된 난이도 적용
        selectedDifficulty = when (defaultDifficultyIndex) {
            0 -> ScoreFeedbackDialogManager.ScoringDifficulty.VERY_EASY
            1 -> ScoreFeedbackDialogManager.ScoringDifficulty.EASY
            2 -> ScoreFeedbackDialogManager.ScoringDifficulty.NORMAL
            3 -> ScoreFeedbackDialogManager.ScoringDifficulty.HARD
            4 -> ScoreFeedbackDialogManager.ScoringDifficulty.VERY_HARD
            else -> ScoreFeedbackDialogManager.ScoringDifficulty.NORMAL
        }

        // 피드백 표시 플래그 리셋
        hasFeedbackShown = false

        // 다이얼로그 없이 바로 카운트다운 후 녹음 시작
        showCountdownAndStartRecording()
    }

    /**
     * 3-2-1 카운트다운 후 녹음 시작
     */
    private fun showCountdownAndStartRecording() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_countdown)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvCountdown = dialog.findViewById<TextView>(R.id.tv_countdown)

        dialog.show()

        // 카운트다운: 3 -> 2 -> 1
        lifecycleScope.launch {
            tvCountdown.text = "3"
            delay(1000)
            tvCountdown.text = "2"
            delay(1000)
            tvCountdown.text = "1"
            delay(1000)
            dialog.dismiss()

            // 녹음 시작 (키 변경 적용)
            val pitchRatio = PitchShiftManager.semitonesToPitchRatio(currentPitchSemitones)
            viewModel.startRecording(pitchArray, pitchRatio)

            // 가이드 오디오 시작 (연습 모드일 때만)
            startGuideAudio()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setObserver() {
        val totalTimeFormatted = formatMillisToTime(durationMillis)
        binding.timeDisplay.text = "00:00 / $totalTimeFormatted"

        // State Flow collection
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.stateFlow.collect { state ->
                    // Update elapsed time
                    val elapsedMs = state.elapsedTime
                    val clampedElapsed = elapsedMs.coerceAtMost(durationMillis)
                    val elapsed = formatMillisToTime(clampedElapsed)
                    val total = formatMillisToTime(durationMillis)
                    binding.timeDisplay.text = "$elapsed / $total"

                    // 원본 피치를 시간에 맞춰 그리기 (녹음 중일 때 자동으로)
                    val index = (clampedElapsed / 100).toInt()
                    if (index > lastOriginIndex && state.isRecording && !state.isPaused) {
                        for (i in lastOriginIndex + 1..index) {
                            val pitch = pitchArray.getOrNull(i) ?: 0f
                            addOriginPitchEntry(pitch, i / 10f)
                        }
                        lastOriginIndex = index

                        // 차트 업데이트 (사용자가 말하지 않아도 자동으로 스크롤)
                        val chart = binding.pitchChart
                        val data = chart.data
                        if (data != null) {
                            data.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            chart.setVisibleXRangeMaximum(10f)
                            val currentTimeSec = clampedElapsed / 1000f
                            chart.moveViewToX((currentTimeSec - 5f).coerceAtLeast(0f))
                            chart.invalidate()
                        }
                    }

                    if (clampedElapsed >= durationMillis && state.isRecording && !state.isPaused) {
                        viewModel.stopRecording()
                    }

                    // Update current pitch
                    val pitch = state.currentPitch
                    val diff = state.pitchDifference
                    if (state.isRecording && !state.isPaused) {
                        binding.pitchDifference.text = "%.1f Hz (오차: %.1f Hz)".format(pitch, diff)
                    } else {
                        binding.pitchDifference.text = "대기 중..."
                    }

                    if (pitch > 0 && !state.isPaused) {
                        addUserPitchEntry(pitch, elapsedMs / 1000f)
                    }

                    // Update recording/pause state UI
                    updateRecordingUI(state.isRecording, state.isPaused)

                    // Update realtime score
                    if (state.isRecording) {
                        binding.scoreCard.visibility = View.VISIBLE
                        binding.currentScore.text = state.currentScore.toString()
                        binding.accuracyText.text = "%.1f%%".format(state.accuracy)
                    } else {
                        binding.scoreCard.visibility = View.GONE
                    }

                    // Update final score
                    state.score?.let { baseScore ->
                        // 이미 피드백을 보여줬으면 중복 방지
                        if (hasFeedbackShown) return@let

                        // ScoreAnalyzer 가져오기
                        val analyzer = viewModel.getScoreAnalyzer()
                        val difficulty = selectedDifficulty ?: ScoreFeedbackDialogManager.ScoringDifficulty.NORMAL

                        if (analyzer != null) {
                            // 저장된 난이도로 점수 조정
                            val adjustedScore = ScoreFeedbackDialogManager.calculateAdjustedScore(baseScore, difficulty)

                            // 점수 저장 (기존 방식)
                            scoreViewModel.saveScore(music.title, adjustedScore, music.artist)

                            // 히스토리 저장 및 게임 보상 처리
                            lifecycleScope.launch {
                                // GameManager 초기화 완료 대기
                                gameManagerInitJob?.join()

                                val detailedScores = analyzer.getDetailedScores()
                                val vibratoInfo = analyzer.detectVibrato()

                                // RecordingHistoryEntity 생성
                                val userId = com.example.musicplayer.manager.AuthManager.getCurrentUserId() ?: "guest"
                                val recordingHistory = RecordingHistoryEntity(
                                    userId = userId,
                                    songName = music.title,
                                    songArtist = music.artist,
                                    songDuration = durationMillis,
                                    totalScore = adjustedScore,
                                    pitchAccuracy = detailedScores["pitch_accuracy"] ?: 0.0,
                                    rhythmScore = detailedScores["rhythm_score"] ?: 0.0,
                                    volumeStability = detailedScores["volume_stability"] ?: 0.0,
                                    durationMatch = detailedScores["duration_match"] ?: 0.0,
                                    hasVibrato = vibratoInfo.hasVibrato,
                                    vibratoScore = vibratoInfo.score,
                                    difficulty = difficulty.name,
                                    recordingFilePath = "" // 녹음 파일은 현재 구현 안됨
                                )

                                // 게임 보상 계산
                                val gameReward = gameManager.onRecordingCompleted(
                                    songName = music.title,
                                    score = adjustedScore,
                                    difficulty = difficulty.name,
                                    recordingHistory = recordingHistory
                                )

                                // 피드백 다이얼로그 표시 (게임 보상 포함)
                                ScoreFeedbackDialogManager.showScoreFeedbackDialog(
                                    context = requireContext(),
                                    analyzer = analyzer,
                                    finalScore = adjustedScore,
                                    difficulty = difficulty,
                                    songTitle = music.title,
                                    gameReward = gameReward
                                )
                            }

                            // 피드백 표시 완료 플래그 설정
                            hasFeedbackShown = true
                        } else {
                            // Fallback: analyzer가 없으면 토스트로 표시
                            ToastManager.showToast("점수 계산 중 오류가 발생했습니다")
                        }
                    }
                }
            }
        }

        // Side effect collection
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.sideEffectFlow.collect { sideEffect ->
                    when (sideEffect) {
                        is RecordingSideEffect.ClearChart -> {
                            // 차트 초기화
                            lastUserX = -1f
                            lastOriginIndex = -1
                            initPitchChart()
                        }
                        is RecordingSideEffect.ShowError -> {
                            ToastManager.showToast(sideEffect.message)
                        }
                    }
                }
            }
        }
    }

    private fun updateRecordingUI(isRecording: Boolean, isPaused: Boolean) {
        if (isRecording) {
            if (isPaused) {
                // 일시정지 상태
                binding.recordingStatus.text = "일시정지"
                binding.btnStart.visibility = View.GONE
                binding.btnPause.visibility = View.VISIBLE
                binding.btnStop.visibility = View.VISIBLE
                binding.btnPause.text = "▶"
                binding.btnPause.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_green_light
                )
            } else {
                // 녹음 중
                binding.recordingStatus.text = "녹음 중..."
                binding.btnStart.visibility = View.GONE
                binding.btnPause.visibility = View.VISIBLE
                binding.btnStop.visibility = View.VISIBLE
                binding.btnPause.text = "⏸"
                binding.btnPause.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_orange_dark
                )
            }
            wasRecording = true
        } else {
            // 녹음 대기 중
            binding.recordingStatus.text = "녹음 대기 중"
            binding.btnStart.visibility = View.VISIBLE
            binding.btnPause.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            wasRecording = false
        }
    }

    private fun addUserPitchEntry(userPitch: Float, xSec: Float) {
        val chart = binding.pitchChart
        val data = chart.data ?: return
        val userDataSet = data.getDataSetByIndex(0)

        if (xSec <= lastUserX) return  // 역순 방지
        lastUserX = xSec

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
    }

    private fun adjustYAxisIfNeeded(newPitch: Float) {
        val chart = binding.pitchChart
        val axis = chart.axisLeft
        if (newPitch > axis.axisMaximum) {
            val newMax = (newPitch * 1.1f).coerceAtLeast(100f)
            axis.axisMaximum = newMax

            // Y축 변경 시 부드러운 애니메이션
            chart.animateY(500, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    }

    private fun trimAndRefreshChart(data: LineData) {
        val chart = binding.pitchChart
        val userDataSet = data.getDataSetByLabel("내 음정", true) ?: return

        // 사용자 피치만 트림 (원본은 전체 유지)
        if (userDataSet.entryCount > 100 && userDataSet.entryCount > 1) {
            userDataSet.removeFirst()
        }

        if (userDataSet.entryCount < 1) return

        data.notifyDataChanged()
        chart.notifyDataSetChanged()

        // 화면에 보이는 범위를 10초로 설정
        chart.setVisibleXRangeMaximum(10f)

        // 현재 시간을 따라가도록 차트 이동 (부드러운 스크롤)
        if (userDataSet.entryCount > 0) {
            val lastX = userDataSet.getEntryForIndex(userDataSet.entryCount - 1).x
            val targetX = (lastX - 5f).coerceAtLeast(0f) // 중앙에 현재 시간 배치

            // 부드러운 애니메이션으로 이동
            chart.moveViewToAnimated(targetX, 0f, com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT, 300)
        }

        chart.invalidate()
    }

    private fun initPitchChart() {
        val chart = binding.pitchChart

        // 차트 기본 설정
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false) // 그리드 배경 제거 (더 깔끔하게)
        chart.setDrawBorders(false) // 테두리 제거
        chart.axisRight.isEnabled = false
        chart.setExtraOffsets(10f, 20f, 10f, 10f) // 여백 추가

        // 범례 스타일 개선
        chart.legend.isEnabled = true
        chart.legend.textSize = 13f
        chart.legend.textColor = android.graphics.Color.parseColor("#37474F")
        chart.legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
        chart.legend.formSize = 16f
        chart.legend.formLineWidth = 3f
        chart.legend.xEntrySpace = 12f
        chart.legend.yEntrySpace = 8f
        chart.legend.formToTextSpace = 8f

        // Y축 설정 - 더 세련된 스타일
        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 300f // 초기 최대값
        leftAxis.textColor = android.graphics.Color.parseColor("#78909C")
        leftAxis.gridColor = android.graphics.Color.parseColor("#ECEFF1")
        leftAxis.gridLineWidth = 1f
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawAxisLine(false) // 축 라인 제거
        leftAxis.textSize = 11f
        leftAxis.granularity = 50f
        leftAxis.setLabelCount(6, false)

        // X축 설정 - 더 세련된 스타일
        val xAxis = chart.xAxis
        xAxis.isEnabled = true
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = android.graphics.Color.parseColor("#ECEFF1")
        xAxis.gridLineWidth = 1f
        xAxis.textColor = android.graphics.Color.parseColor("#78909C")
        xAxis.setDrawAxisLine(false) // 축 라인 제거
        xAxis.granularity = 1f
        xAxis.labelCount = 5
        xAxis.textSize = 11f
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "%.0f초".format(value)
        }

        // 사용자 피치 데이터셋 - 부드러운 곡선과 그라디언트
        val userDataSet = LineDataSet(mutableListOf(), "내 음정").apply {
            // 색상 - 더 선명한 파란색
            color = android.graphics.Color.parseColor("#1976D2")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 3.5f

            // 부드러운 곡선 (Cubic Bezier)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f

            // 그라디언트 채우기
            setDrawFilled(true)
            fillColor = android.graphics.Color.parseColor("#1976D2")
            fillAlpha = 40

            // 그림자 효과 (하이라이트)
            setDrawHighlightIndicators(false)
            isHighlightEnabled = false
        }

        // 원본 피치 데이터셋 - 부드러운 곡선과 그라디언트
        val originalDataSet = LineDataSet(mutableListOf(), "원곡 멜로디").apply {
            // 색상 - 더 선명한 오렌지
            color = android.graphics.Color.parseColor("#F57C00")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2.5f

            // 부드러운 곡선 (Cubic Bezier)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f

            // 그라디언트 채우기
            setDrawFilled(true)
            fillColor = android.graphics.Color.parseColor("#FFB74D")
            fillAlpha = 60

            // 점선 스타일 (원곡은 점선으로)
            enableDashedLine(10f, 5f, 0f)

            // 그림자 효과 (하이라이트)
            setDrawHighlightIndicators(false)
            isHighlightEnabled = false
        }

        chart.data = LineData(userDataSet, originalDataSet)

        // 초기 애니메이션 추가 (부드럽게 나타남)
        chart.animateXY(1000, 1000, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
    }

    @SuppressLint("DefaultLocale")
    private fun formatMillisToTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopRecording()
        releaseGuidePlayer()
        gameManager.release()
        _binding = null
    }

    companion object {
        fun newInstance(
            music: MusicFile,
            originalPitch: FloatArray,
            durationMillis: Long,
            singingMode: SingingMode = SingingMode.PRACTICE
        ): RecordingFragment {
            return RecordingFragment().apply {
                arguments = bundleOf(
                    "music" to music,
                    "pitchArray" to originalPitch,
                    "durationMillis" to durationMillis,
                    "singingMode" to singingMode.name
                )
            }
        }
    }
}
