package com.example.musicplayer.fragment

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.error.AppException
import com.example.musicplayer.error.ErrorHandler
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.scoreAlgorythm.ScoreAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class RecordingViewModel : ViewModel() {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    val isRecording = MutableLiveData(false)
    val currentPitch = MutableLiveData<Float>()
    val pitchDifference = MutableLiveData<Float>()
    val elapsedTime = MutableLiveData<Long>()
    val score = MutableLiveData<Int>()
    val clearChartTrigger = MutableLiveData<Unit>()

    private var dispatcher: AudioDispatcher? = null
    private var pitchThread: Thread? = null
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null

    private var currentPitchArray: FloatArray = floatArrayOf()
    private var startTimeMillis: Long = 0
    private val pitchPairs = mutableListOf<Pair<Float, Float>>()
    private val pitchBuffer = mutableListOf<Pair<Float, Float>>()
    private val bufferSize = 10

    fun startRecording(pitchArray: FloatArray) {
        if (isRecording.value == true) {
            throw AppException.InvalidStateException("Recording is already in progress")
        }

        try {
            isRecording.postValue(true)
            currentPitchArray = pitchArray
            startTimeMillis = System.currentTimeMillis()
            elapsedTime.postValue(0)
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
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val ms = (System.currentTimeMillis() - startTimeMillis)
                elapsedTime.postValue(ms)
                timerHandler?.postDelayed(this, 100)
            }
        }
        timerHandler?.postDelayed(timerRunnable!!, 100)
    }

    private fun setupAudioDispatcher() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            22050f,
            1024
        ) { result, _ ->
            audioScope.launch {
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

                        withContext(Dispatchers.Main) {
                            currentPitch.postValue(userPitch)
                            pitchDifference.postValue(abs(userPitch - targetPitch))
                        }
                    }
                } catch (e: Exception) {
                    LogManager.e("Error processing pitch: ${e.message}")
                }
            }
        }

        dispatcher?.addAudioProcessor(pitchProcessor)
        pitchThread = Thread(dispatcher, "Pitch Thread")
        pitchThread?.start()
    }

    fun stopRecording() {
        try {
            isRecording.postValue(false)
            cleanupResources()
            calculateScore()
        } catch (e: Exception) {
            LogManager.e("Failed to stop recording: ${e.message}")
            throw AppException.AudioRecordingException("Failed to stop recording", e)
        }
    }

    private fun cleanupResources() {
        dispatcher?.stop()
        dispatcher = null
        pitchThread = null

        timerHandler?.removeCallbacks(timerRunnable!!)
        timerHandler = null
        timerRunnable = null

        elapsedTime.postValue(0)
        currentPitch.postValue(0f)
        pitchDifference.postValue(0f)
        clearChartTrigger.postValue(Unit)

        // 남은 버퍼 데이터 처리
        if (pitchBuffer.isNotEmpty()) {
            pitchPairs.addAll(pitchBuffer)
            pitchBuffer.clear()
        }
    }

    private fun calculateScore() {
        viewModelScope.launch {
            try {
                val originalPitchList = pitchPairs.map { it.first }
                val userPitchList = pitchPairs.map { it.second }

                val analyzer = ScoreAnalyzer(originalPitchList, userPitchList)
                val finalScore = analyzer.calculateTotalScore()

                score.postValue(finalScore)
            } catch (e: Exception) {
                LogManager.e("Failed to calculate score: ${e.message}")
                throw AppException.ScoreCalculationException("Failed to calculate score", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupResources()
        viewModelScope.cancel()
        audioScope.cancel()
    }

    // 테스트를 위한 메서드
    fun getPitchPairs(): List<Pair<Float, Float>> = pitchPairs.toList()
}



