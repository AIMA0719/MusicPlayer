package com.example.musicplayer.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.error.AppException
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.scoreAlgorithm.ScoreAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import kotlin.math.abs

class RecordingViewModel : ViewModel(), ContainerHost<RecordingState, RecordingSideEffect> {

    override val container: Container<RecordingState, RecordingSideEffect> =
        container(RecordingState())

    private var dispatcher: AudioDispatcher? = null
    private var timerJob: kotlinx.coroutines.Job? = null
    private var amplitudeJob: kotlinx.coroutines.Job? = null

    private var currentPitchArray: FloatArray = floatArrayOf()
    private var startTimeMillis: Long = 0
    private var pausedTimeTotal: Long = 0
    private var pauseStartTime: Long = 0
    private val pitchPairs = mutableListOf<Pair<Float, Float>>()
    private val pitchBuffer = mutableListOf<Pair<Float, Float>>()
    private val bufferSize = 10

    // ScoreAnalyzer 저장
    private var scoreAnalyzer: ScoreAnalyzer? = null

    // 피치 시프트 비율 (1.0 = 원본, > 1.0 = 높은 키, < 1.0 = 낮은 키)
    private var pitchShiftRatio: Float = 1.0f

    /**
     * 피치 시프트 비율 설정
     * @param ratio 피치 비율 (예: 1.0594631 = +1 반음)
     */
    fun setPitchShiftRatio(ratio: Float) {
        pitchShiftRatio = ratio
    }

    fun startRecording(pitchArray: FloatArray, pitchRatio: Float = 1.0f) = intent {
        if (state.isRecording) {
            throw AppException.InvalidStateException("Recording is already in progress")
        }

        try {
            reduce {
                state.copy(
                    isRecording = true,
                    isPaused = false,
                    elapsedTime = 0,
                    score = null,
                    currentScore = 0,
                    accuracy = 0f
                )
            }

            // 피치 시프트 비율 저장
            pitchShiftRatio = pitchRatio

            // 원본 피치 배열에 피치 시프트 적용
            currentPitchArray = if (pitchRatio != 1.0f) {
                pitchArray.map { pitch ->
                    if (pitch > 0) pitch * pitchRatio else pitch
                }.toFloatArray()
            } else {
                pitchArray
            }

            startTimeMillis = System.currentTimeMillis()
            pausedTimeTotal = 0
            pauseStartTime = 0
            pitchPairs.clear()
            pitchBuffer.clear()

            setupTimer()
            setupAmplitudeMonitor()
            setupAudioDispatcher()
        } catch (e: Exception) {
            LogManager.e("Failed to start recording: ${e.message}")
            stopRecording()
            throw AppException.AudioRecordingException("Failed to start recording", e)
        }
    }

    private fun setupTimer() {
        timerJob = viewModelScope.launch {
            while (container.stateFlow.value.isRecording) {
                if (!container.stateFlow.value.isPaused) {
                    val ms = (System.currentTimeMillis() - startTimeMillis - pausedTimeTotal)
                    intent {
                        reduce { state.copy(elapsedTime = ms) }
                    }
                }
                delay(100)
            }
        }
    }

    private fun setupAmplitudeMonitor() {
        amplitudeJob = viewModelScope.launch {
            while (container.stateFlow.value.isRecording) {
                if (!container.stateFlow.value.isPaused) {
                    // 진폭 시뮬레이션 (실제로는 마이크로부터 가져와야 함)
                    // AudioDispatcher를 통해 가져올 수 있지만 여기서는 간단히 랜덤값 사용
                    val simulatedAmplitude = (1000..5000).random()
                    intent {
                        reduce { state.copy(amplitude = simulatedAmplitude) }
                    }
                }
                delay(100)
            }
        }
    }

    private fun setupAudioDispatcher() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            22050f,
            1024
        ) { result, _ ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // 일시정지 중이면 피치 처리 건너뛰기
                    if (container.stateFlow.value.isPaused) return@launch

                    val ms = (System.currentTimeMillis() - startTimeMillis - pausedTimeTotal).toInt()
                    val index = ms / 100

                    if (index in currentPitchArray.indices) {
                        val targetPitch = currentPitchArray[index]
                        val userPitch = if (result.pitch > 0) result.pitch else 0f

                        pitchBuffer.add(targetPitch to userPitch)

                        if (pitchBuffer.size >= bufferSize) {
                            pitchPairs.addAll(pitchBuffer)
                            pitchBuffer.clear()

                            // 실시간 점수/정확도 계산
                            calculateRealtimeScore()
                        }

                        intent {
                            reduce {
                                state.copy(
                                    currentPitch = userPitch,
                                    pitchDifference = abs(userPitch - targetPitch)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogManager.e("Error processing pitch: ${e.message}")
                }
            }
        }

        dispatcher?.addAudioProcessor(pitchProcessor)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dispatcher?.run()
            } catch (e: Exception) {
                LogManager.e("Audio dispatcher error: ${e.message}")
            }
        }
    }

    private fun calculateRealtimeScore() = intent {
        try {
            if (pitchPairs.isEmpty()) return@intent

            val originalPitchList = pitchPairs.map { it.first }
            val userPitchList = pitchPairs.map { it.second }

            val analyzer = ScoreAnalyzer(originalPitchList, userPitchList)
            val score = analyzer.calculateTotalScore()

            // 정확도 계산 (0-100%)
            val accuracy = if (originalPitchList.isNotEmpty()) {
                val matchCount = originalPitchList.zip(userPitchList).count { (original, user) ->
                    user > 0 && abs(user - original) < 50 // 50Hz 이내면 정확하다고 판단
                }
                (matchCount.toFloat() / originalPitchList.size * 100f).coerceIn(0f, 100f)
            } else {
                0f
            }

            reduce {
                state.copy(
                    currentScore = score,
                    accuracy = accuracy
                )
            }
        } catch (e: Exception) {
            LogManager.e("Failed to calculate realtime score: ${e.message}")
        }
    }

    fun pauseRecording() = intent {
        if (!state.isRecording || state.isPaused) return@intent

        pauseStartTime = System.currentTimeMillis()
        reduce { state.copy(isPaused = true) }
    }

    fun resumeRecording() = intent {
        if (!state.isRecording || !state.isPaused) return@intent

        pausedTimeTotal += (System.currentTimeMillis() - pauseStartTime)
        pauseStartTime = 0
        reduce { state.copy(isPaused = false) }
    }

    fun stopRecording() = intent {
        try {
            reduce { state.copy(isRecording = false) }
            cleanupResources()
            calculateScore()
        } catch (e: Exception) {
            LogManager.e("Failed to stop recording: ${e.message}")
            throw AppException.AudioRecordingException("Failed to stop recording", e)
        }
    }

    private fun cleanupResources() = intent {
        dispatcher?.stop()
        dispatcher = null

        timerJob?.cancel()
        timerJob = null

        amplitudeJob?.cancel()
        amplitudeJob = null

        reduce {
            state.copy(
                isPaused = false,
                elapsedTime = 0,
                currentPitch = 0f,
                pitchDifference = 0f,
                amplitude = 0,
                currentScore = 0,
                accuracy = 0f
            )
        }

        pausedTimeTotal = 0
        pauseStartTime = 0

        postSideEffect(RecordingSideEffect.ClearChart)

        // 남은 버퍼 데이터 처리
        if (pitchBuffer.isNotEmpty()) {
            pitchPairs.addAll(pitchBuffer)
            pitchBuffer.clear()
        }
    }

    private fun calculateScore() = intent {
        try {
            val originalPitchList = pitchPairs.map { it.first }
            val userPitchList = pitchPairs.map { it.second }

            val analyzer = ScoreAnalyzer(originalPitchList, userPitchList)
            scoreAnalyzer = analyzer // Analyzer 저장
            var finalScore = analyzer.calculateTotalScore()

            // 95점 이상이면 50% 확률로 100점 처리
            if (finalScore in 95..<100) {
                val random = kotlin.random.Random.nextBoolean()
                if (random) {
                    finalScore = 100
                    LogManager.d("Score bonus applied: 95+ → 100")
                }
            }

            reduce { state.copy(score = finalScore) }
        } catch (e: Exception) {
            LogManager.e("Failed to calculate score: ${e.message}")
            throw AppException.ScoreCalculationException("Failed to calculate score", e)
        }
    }

    // ScoreAnalyzer 가져오기
    fun getScoreAnalyzer(): ScoreAnalyzer? = scoreAnalyzer

    override fun onCleared() {
        super.onCleared()
        dispatcher?.stop()
        dispatcher = null
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
    }
}
