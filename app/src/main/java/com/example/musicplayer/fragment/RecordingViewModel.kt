package com.example.musicplayer.fragment

import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.error.AppException
import com.example.musicplayer.error.ErrorHandler
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.scoreAlgorythm.ScoreAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var currentPitchArray: FloatArray = floatArrayOf()
    private var startTimeMillis: Long = 0
    private val pitchPairs = mutableListOf<Pair<Float, Float>>()
    private val pitchBuffer = mutableListOf<Pair<Float, Float>>()
    private val bufferSize = 10

    fun startRecording(pitchArray: FloatArray) = intent {
        if (state.isRecording) {
            throw AppException.InvalidStateException("Recording is already in progress")
        }

        try {
            reduce { state.copy(isRecording = true, elapsedTime = 0, score = null) }
            currentPitchArray = pitchArray
            startTimeMillis = System.currentTimeMillis()
            pitchPairs.clear()
            pitchBuffer.clear()

            setupTimer()
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
                val ms = (System.currentTimeMillis() - startTimeMillis)
                intent {
                    reduce { state.copy(elapsedTime = ms) }
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
                    val ms = (System.currentTimeMillis() - startTimeMillis).toInt()
                    val index = ms / 100

                    if (index in currentPitchArray.indices) {
                        val targetPitch = currentPitchArray[index]
                        val userPitch = if (result.pitch > 0) result.pitch else 0f

                        pitchBuffer.add(targetPitch to userPitch)

                        if (pitchBuffer.size >= bufferSize) {
                            pitchPairs.addAll(pitchBuffer)
                            pitchBuffer.clear()
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

        reduce {
            state.copy(
                elapsedTime = 0,
                currentPitch = 0f,
                pitchDifference = 0f
            )
        }

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
            val finalScore = analyzer.calculateTotalScore()

            reduce { state.copy(score = finalScore) }
        } catch (e: Exception) {
            LogManager.e("Failed to calculate score: ${e.message}")
            throw AppException.ScoreCalculationException("Failed to calculate score", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher?.stop()
        dispatcher = null
        timerJob?.cancel()
        timerJob = null
    }

    // 테스트를 위한 메서드
    fun getPitchPairs(): List<Pair<Float, Float>> = pitchPairs.toList()
}
