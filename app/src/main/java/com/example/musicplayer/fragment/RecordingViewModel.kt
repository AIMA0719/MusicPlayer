package com.example.musicplayer.fragment

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.scoreAlgorythm.ScoreAnalyzer
import kotlin.math.abs

class RecordingViewModel : ViewModel() {

    val isRecording = MutableLiveData(false)
    val currentPitch = MutableLiveData<Float>()
    val pitchDifference = MutableLiveData<Float>()
    val elapsedTime = MutableLiveData<Long>()
    val score = MutableLiveData<Int>() // ✅ 최종 점수 LiveData 추가
    val clearChartTrigger = MutableLiveData<Unit>() // 단순 트리거
    private var dispatcher: AudioDispatcher? = null
    private var pitchThread: Thread? = null
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null

    private var currentPitchArray: FloatArray = floatArrayOf()
    private var startTimeMillis: Long = 0

    // ✅ pitch 기록 리스트
    private val pitchPairs = mutableListOf<Pair<Float, Float>>()

    fun startRecording(pitchArray: FloatArray) {
        try {
            if (isRecording.value == true) return

            isRecording.postValue(true)
            currentPitchArray = pitchArray
            startTimeMillis = System.currentTimeMillis()
            elapsedTime.postValue(0)
            pitchPairs.clear() // ✅ 시작 전 기록 초기화

            // 경과 시간 측정용 타이머
            timerHandler = Handler(Looper.getMainLooper())
            timerRunnable = object : Runnable {
                override fun run() {
                    val ms = (System.currentTimeMillis() - startTimeMillis)
                    elapsedTime.postValue(ms)
                    timerHandler?.postDelayed(this, 100)
                }
            }
            timerHandler?.postDelayed(timerRunnable!!, 100)

            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                22050f,
                1024
            ) { result, _ ->
                val ms = (System.currentTimeMillis() - startTimeMillis).toInt()
                val index = ms / 100

                if (index in currentPitchArray.indices) {
                    val targetPitch = currentPitchArray[index]
                    val userPitch = if (result.pitch > 0) result.pitch else 0f

                    pitchPairs.add(targetPitch to userPitch)

                    currentPitch.postValue(userPitch)
                    pitchDifference.postValue(abs(userPitch - targetPitch))
                }
            }

            dispatcher?.addAudioProcessor(pitchProcessor)
            pitchThread = Thread(dispatcher, "Pitch Thread")
            pitchThread?.start()
        } catch (e: Exception) {
            stopRecording()
            ToastManager.show("녹음 시작 중 오류가 발생했습니다: ${e.message}")
        }
    }

    fun stopRecording() {
        try {
            isRecording.postValue(false)
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

            // pitchPairs를 분리해서 ScoreAnalyzer에 넘기고 점수 계산
            val originalPitchList = pitchPairs.map { it.first }
            val userPitchList = pitchPairs.map { it.second }

            val analyzer = ScoreAnalyzer(originalPitchList, userPitchList)
            val finalScore = analyzer.calculateTotalScore()

            score.postValue(finalScore)
        } catch (e: Exception) {
            ToastManager.show("녹음 중 오류가 발생했습니다: ${e.message}")
        }
    }
}



