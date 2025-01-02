package com.example.musicplayer.ScoreAlgorythm

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.example.musicplayer.Manager.LogManager

class ScoreCalculator {

    fun calculateScore(referenceMp4: String, userMp4: String): Int {
        // Step 1: Convert MP4 to WAV using FFmpeg
        val referenceWav = convertMp4ToWav(referenceMp4)
        val userWav = convertMp4ToWav(userMp4)

        // Step 2: Extract pitch data
        val referencePitches = extractPitchData(referenceWav)
        val userPitches = extractPitchData(userWav)

        // Step 3: Analyze similarity and calculate score
        val pitchAccuracy = calculatePitchAccuracy(referencePitches, userPitches)

        return (pitchAccuracy * 100).toInt()
    }

    private fun convertMp4ToWav(mp4Path: String): String {
        val wavPath = mp4Path.replace(".mp4", ".wav")
        val command = "-i $mp4Path -ar 44100 -ac 1 -y $wavPath"

        val result = com.arthenica.mobileffmpeg.FFmpeg.execute(command)
        if (result != 0) {
            throw RuntimeException("Error converting MP4 to WAV: $result")
        }

        return wavPath
    }

    private fun extractPitchData(wavPath: String): List<Float> {
        val pitchList = mutableListOf<Float>()

        LogManager.e("wavPath : $wavPath")
        val dispatcher: AudioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100, 2048, 1024)
        val pitchHandler = PitchDetectionHandler { result, _ ->
            if (result.pitch != -1f) {
                pitchList.add(result.pitch)
            }
        }

        val pitchProcessor = PitchProcessor(PitchEstimationAlgorithm.YIN, 44100f, 2048, pitchHandler)
        dispatcher.addAudioProcessor(pitchProcessor)

        val thread = Thread {
            dispatcher.run()
        }
        thread.start()
        thread.join() // Dispatcher가 종료될 때까지 대기

        return pitchList
    }


    private fun calculatePitchAccuracy(referencePitches: List<Float>, userPitches: List<Float>): Double {
        val minSize = minOf(referencePitches.size, userPitches.size)

        var correctCount = 0
        for (i in 0 until minSize) {
            val refPitch = referencePitches[i]
            val userPitch = userPitches[i]

            if (Math.abs(refPitch - userPitch) <= 50) { // 허용 오차 50Hz
                correctCount++
            }
        }

        return correctCount.toDouble() / minSize
    }
}
