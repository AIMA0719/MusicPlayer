package com.example.musicplayer.manager

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * 키 변경(Pitch Shift) 관리자
 *
 * - 반음 단위로 키 변경 (-6 ~ +6)
 * - 속도는 유지하면서 피치만 변경
 * - Sonic 알고리즘 사용 (Time Stretching + Pitch Shifting)
 */
class PitchShiftManager(private val context: Context) {

    companion object {
        const val MIN_PITCH_SEMITONES = -6
        const val MAX_PITCH_SEMITONES = 6
        const val DEFAULT_PITCH_SEMITONES = 0

        // 반음 → 피치 비율 변환 (12-TET)
        fun semitonesToPitchRatio(semitones: Int): Float {
            return 2f.pow(semitones / 12f)
        }

        // 반음 → 키 표시 문자열
        fun semitonesToKeyString(semitones: Int): String {
            return when {
                semitones > 0 -> "+$semitones"
                semitones < 0 -> "$semitones"
                else -> "0 (원키)"
            }
        }
    }

    // 현재 피치 설정 (반음 단위)
    private var currentSemitones: Int = DEFAULT_PITCH_SEMITONES

    // 재생 상태
    private var isPlaying = false
    private var isPaused = false
    private var currentUri: Uri? = null

    // 코루틴 스코프
    private var playbackScope: CoroutineScope? = null
    private var playbackJob: Job? = null

    // 미디어 관련
    private var audioTrack: AudioTrack? = null
    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null

    // Sonic 프로세서 (피치 시프팅)
    private var sonic: Sonic? = null

    // 현재 재생 위치 (밀리초)
    private var currentPositionMs: Long = 0
    private var totalDurationMs: Long = 0

    // 콜백
    var onPlaybackStateChanged: ((isPlaying: Boolean, isPaused: Boolean) -> Unit)? = null
    var onPositionChanged: ((positionMs: Long, durationMs: Long) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * 현재 키 설정 가져오기 (반음 단위)
     */
    fun getCurrentSemitones(): Int = currentSemitones

    /**
     * 키 설정 변경 (반음 단위)
     */
    fun setSemitones(semitones: Int) {
        val clamped = semitones.coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES)
        if (currentSemitones != clamped) {
            currentSemitones = clamped
            sonic?.pitch = semitonesToPitchRatio(clamped)
            Timber.d("Pitch changed to $clamped semitones (ratio: ${semitonesToPitchRatio(clamped)})")
        }
    }

    /**
     * 키 올리기 (+1 반음)
     */
    fun pitchUp(): Int {
        setSemitones(currentSemitones + 1)
        return currentSemitones
    }

    /**
     * 키 내리기 (-1 반음)
     */
    fun pitchDown(): Int {
        setSemitones(currentSemitones - 1)
        return currentSemitones
    }

    /**
     * 원래 키로 리셋
     */
    fun resetPitch(): Int {
        setSemitones(DEFAULT_PITCH_SEMITONES)
        return currentSemitones
    }

    /**
     * 재생 시작
     */
    fun play(uri: Uri) {
        // 이미 재생 중이면 정지
        stop()

        currentUri = uri
        playbackScope = CoroutineScope(Dispatchers.IO)

        playbackJob = playbackScope?.launch {
            try {
                playAudioWithPitchShift(uri)
            } catch (e: Exception) {
                Timber.e(e, "Playback error")
                onError?.invoke("재생 오류: ${e.message}")
            }
        }
    }

    /**
     * 일시정지
     */
    fun pause() {
        if (isPlaying && !isPaused) {
            isPaused = true
            audioTrack?.pause()
            onPlaybackStateChanged?.invoke(isPlaying, isPaused)
        }
    }

    /**
     * 재개
     */
    fun resume() {
        if (isPlaying && isPaused) {
            isPaused = false
            audioTrack?.play()
            onPlaybackStateChanged?.invoke(isPlaying, isPaused)
        }
    }

    /**
     * 정지
     */
    fun stop() {
        isPlaying = false
        isPaused = false

        playbackJob?.cancel()
        playbackJob = null
        playbackScope?.cancel()
        playbackScope = null

        releaseResources()

        onPlaybackStateChanged?.invoke(false, false)
    }

    /**
     * 특정 위치로 이동
     */
    fun seekTo(positionMs: Long) {
        mediaExtractor?.seekTo(positionMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        currentPositionMs = positionMs
    }

    /**
     * 현재 재생 중인지 여부
     */
    fun isPlaying(): Boolean = isPlaying && !isPaused

    /**
     * 현재 위치 (밀리초)
     */
    fun getCurrentPosition(): Long = currentPositionMs

    /**
     * 총 길이 (밀리초)
     */
    fun getDuration(): Long = totalDurationMs

    /**
     * 리소스 해제
     */
    fun release() {
        stop()
    }

    /**
     * 피치 시프팅을 적용하여 오디오 재생
     */
    private suspend fun playAudioWithPitchShift(uri: Uri) {
        // MediaExtractor 설정
        mediaExtractor = MediaExtractor().apply {
            setDataSource(context, uri, null)
        }

        // 오디오 트랙 찾기
        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until mediaExtractor!!.trackCount) {
            val format = mediaExtractor!!.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
            onError?.invoke("오디오 트랙을 찾을 수 없습니다")
            return
        }

        mediaExtractor!!.selectTrack(audioTrackIndex)

        // 오디오 포맷 정보
        val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        totalDurationMs = audioFormat.getLong(MediaFormat.KEY_DURATION) / 1000

        // MediaCodec 설정
        val mime = audioFormat.getString(MediaFormat.KEY_MIME)!!
        mediaCodec = MediaCodec.createDecoderByType(mime).apply {
            configure(audioFormat, null, null, 0)
            start()
        }

        // AudioTrack 설정
        val channelConfig = if (channelCount == 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
            AudioTrack.MODE_STREAM
        ).apply {
            play()
        }

        // Sonic 초기화 (피치 시프팅)
        sonic = Sonic(sampleRate, channelCount).apply {
            pitch = semitonesToPitchRatio(currentSemitones)
            speed = 1.0f // 속도는 유지
        }

        isPlaying = true
        isPaused = false
        onPlaybackStateChanged?.invoke(true, false)

        // 디코딩 및 재생 루프
        val inputBuffers = mediaCodec!!.inputBuffers
        var outputBuffers = mediaCodec!!.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        try {
            while (isPlaying && playbackScope?.isActive == true) {
                // 일시정지 상태면 대기
                if (isPaused) {
                    Thread.sleep(50)
                    continue
                }

                // 입력 버퍼에 데이터 쓰기
                if (!isEOS) {
                    val inputIndex = mediaCodec!!.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = inputBuffers[inputIndex]
                        val sampleSize = mediaExtractor!!.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            mediaCodec!!.queueInputBuffer(
                                inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEOS = true
                        } else {
                            val presentationTimeUs = mediaExtractor!!.sampleTime
                            mediaCodec!!.queueInputBuffer(
                                inputIndex, 0, sampleSize, presentationTimeUs, 0
                            )
                            mediaExtractor!!.advance()
                        }
                    }
                }

                // 출력 버퍼에서 데이터 읽기
                val outputIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        @Suppress("DEPRECATION")
                        outputBuffers = mediaCodec!!.outputBuffers
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // 포맷 변경됨
                    }
                    outputIndex >= 0 -> {
                        val outputBuffer = outputBuffers[outputIndex]
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        outputBuffer.clear()

                        // 피치 시프팅 적용
                        val processedData = processPitchShift(chunk, channelCount)

                        // AudioTrack에 쓰기
                        if (processedData.isNotEmpty()) {
                            audioTrack?.write(processedData, 0, processedData.size)
                        }

                        // 현재 위치 업데이트
                        currentPositionMs = bufferInfo.presentationTimeUs / 1000
                        onPositionChanged?.invoke(currentPositionMs, totalDurationMs)

                        mediaCodec!!.releaseOutputBuffer(outputIndex, false)

                        // 끝에 도달
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            // Sonic 플러시
                            flushSonic(channelCount)
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Playback loop error")
        }

        // 재생 완료
        if (isPlaying) {
            isPlaying = false
            onPlaybackCompleted?.invoke()
        }

        releaseResources()
    }

    /**
     * 피치 시프팅 처리
     */
    private fun processPitchShift(input: ByteArray, numChannels: Int): ByteArray {
        if (sonic == null || input.isEmpty()) return input

        // byte[] → short[] 변환
        val shortBuffer = ByteBuffer.wrap(input)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val shorts = ShortArray(input.size / 2)
        shortBuffer.get(shorts)

        // Sonic 처리
        sonic!!.writeShortsToStream(shorts, shorts.size / numChannels)

        // 결과 읽기
        val outputShorts = ShortArray(shorts.size * 2) // 여유 공간
        val numSamples = sonic!!.readShortsFromStream(outputShorts, outputShorts.size / numChannels)

        if (numSamples <= 0) return ByteArray(0)

        // short[] → byte[] 변환
        val outputBytes = ByteArray(numSamples * numChannels * 2)
        val byteBuffer = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples * numChannels) {
            byteBuffer.putShort(outputShorts[i])
        }

        return outputBytes
    }

    /**
     * Sonic 버퍼 플러시
     */
    private fun flushSonic(numChannels: Int) {
        sonic?.flushStream()
        val outputShorts = ShortArray(8192)
        var numSamples: Int

        while (true) {
            numSamples = sonic?.readShortsFromStream(outputShorts, outputShorts.size / numChannels) ?: 0
            if (numSamples <= 0) break

            val outputBytes = ByteArray(numSamples * numChannels * 2)
            val byteBuffer = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numSamples * numChannels) {
                byteBuffer.putShort(outputShorts[i])
            }
            audioTrack?.write(outputBytes, 0, outputBytes.size)
        }
    }

    /**
     * 리소스 해제
     */
    private fun releaseResources() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Timber.e(e, "AudioTrack release error")
        }

        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            Timber.e(e, "MediaCodec release error")
        }

        try {
            mediaExtractor?.release()
            mediaExtractor = null
        } catch (e: Exception) {
            Timber.e(e, "MediaExtractor release error")
        }

        sonic = null
    }
}

/**
 * Sonic 알고리즘 구현 (Time Stretching + Pitch Shifting)
 *
 * 기반: Sonic Library (https://github.com/nickoppen/Sonic.java)
 * 라이선스: Apache 2.0
 */
class Sonic(
    private val sampleRate: Int,
    private val numChannels: Int
) {
    private var inputBuffer: ShortArray = ShortArray(0)
    private var outputBuffer: ShortArray = ShortArray(0)
    private var pitchBuffer: ShortArray = ShortArray(0)
    private var downSampleBuffer: ShortArray = ShortArray(0)

    private var inputBufferSize = 0
    private var outputBufferSize = 0
    private var pitchBufferSize = 0

    private var numInputSamples = 0
    private var numOutputSamples = 0
    private var numPitchSamples = 0

    private var minPeriod = 0
    private var maxPeriod = 0
    private var maxRequired = 0
    private var remainingInputToCopy = 0
    private var prevPeriod = 0
    private var prevMinDiff = 0
    private var newRatePosition = 0
    private var oldRatePosition = 0

    var speed: Float = 1.0f
    var pitch: Float = 1.0f
    var rate: Float = 1.0f
    var volume: Float = 1.0f
    var chordPitch: Boolean = false
    var quality: Int = 0

    init {
        allocateStreamBuffers()
    }

    private fun allocateStreamBuffers() {
        minPeriod = sampleRate / 400
        maxPeriod = sampleRate / 65
        maxRequired = 2 * maxPeriod

        inputBufferSize = maxRequired
        inputBuffer = ShortArray(maxRequired * numChannels)
        outputBufferSize = maxRequired
        outputBuffer = ShortArray(maxRequired * numChannels)
        pitchBufferSize = maxRequired
        pitchBuffer = ShortArray(maxRequired * numChannels)
        downSampleBuffer = ShortArray(maxRequired)
    }

    private fun enlargeOutputBufferIfNeeded(numSamples: Int) {
        if (numOutputSamples + numSamples > outputBufferSize) {
            outputBufferSize += (outputBufferSize shr 1) + numSamples
            outputBuffer = outputBuffer.copyOf(outputBufferSize * numChannels)
        }
    }

    private fun enlargeInputBufferIfNeeded(numSamples: Int) {
        if (numInputSamples + numSamples > inputBufferSize) {
            inputBufferSize += (inputBufferSize shr 1) + numSamples
            inputBuffer = inputBuffer.copyOf(inputBufferSize * numChannels)
        }
    }

    private fun addShortSamplesToInputBuffer(samples: ShortArray, numSamples: Int) {
        if (numSamples == 0) return
        enlargeInputBufferIfNeeded(numSamples)
        System.arraycopy(
            samples, 0,
            inputBuffer, numInputSamples * numChannels,
            numSamples * numChannels
        )
        numInputSamples += numSamples
    }

    private fun removeInputSamples(position: Int) {
        val remainingSamples = numInputSamples - position
        System.arraycopy(
            inputBuffer, position * numChannels,
            inputBuffer, 0,
            remainingSamples * numChannels
        )
        numInputSamples = remainingSamples
    }

    private fun copyToOutput(samples: ShortArray, position: Int, numSamples: Int) {
        enlargeOutputBufferIfNeeded(numSamples)
        System.arraycopy(
            samples, position * numChannels,
            outputBuffer, numOutputSamples * numChannels,
            numSamples * numChannels
        )
        numOutputSamples += numSamples
    }

    private fun copyInputToOutput(position: Int): Int {
        val numSamples = remainingInputToCopy
        if (numSamples > maxRequired) {
            val extra = numSamples - maxRequired
            copyToOutput(inputBuffer, position, maxRequired)
            remainingInputToCopy = extra
            return maxRequired
        }
        copyToOutput(inputBuffer, position, numSamples)
        remainingInputToCopy = 0
        return numSamples
    }

    private fun downSampleInput(samples: ShortArray, position: Int, skip: Int): Int {
        val numSamples = maxRequired / skip
        val samplesPerValue = numChannels * skip
        var i = 0
        var j = position * numChannels

        while (i < numSamples) {
            var value = 0
            for (k in 0 until samplesPerValue) {
                value += samples[j + k]
            }
            downSampleBuffer[i] = (value / samplesPerValue).toShort()
            i++
            j += samplesPerValue
        }
        return numSamples
    }

    private fun findPitchPeriodInRange(
        samples: ShortArray,
        position: Int,
        minPeriod: Int,
        maxPeriod: Int
    ): Int {
        var bestPeriod = 0
        var worstPeriod = 255
        var minDiff = 1
        var maxDiff = 0

        for (period in minPeriod..maxPeriod) {
            var diff = 0
            for (i in 0 until period) {
                val sVal = samples[position + i]
                val pVal = samples[position + period + i]
                diff += kotlin.math.abs(sVal - pVal)
            }

            if (diff * bestPeriod < minDiff * period) {
                minDiff = diff
                bestPeriod = period
            }
            if (diff * worstPeriod > maxDiff * period) {
                maxDiff = diff
                worstPeriod = period
            }
        }

        prevMinDiff = minDiff
        prevPeriod = bestPeriod
        return bestPeriod
    }

    private fun findPitchPeriod(samples: ShortArray, position: Int, preferNewPeriod: Boolean): Int {
        var period: Int
        var retPeriod: Int
        val skip = if (numInputSamples > SAMPLE_RATE) numInputSamples / SAMPLE_RATE else 1

        if (numChannels == 1 && skip == 1) {
            period = findPitchPeriodInRange(samples, position, minPeriod, maxPeriod)
        } else {
            downSampleInput(samples, position, skip)
            period = findPitchPeriodInRange(
                downSampleBuffer, 0,
                minPeriod / skip, maxPeriod / skip
            )
            if (skip != 1) {
                period *= skip
                var minP = period - (skip shl 2)
                var maxP = period + (skip shl 2)
                if (minP < minPeriod) minP = minPeriod
                if (maxP > maxPeriod) maxP = maxPeriod
                if (numChannels == 1) {
                    period = findPitchPeriodInRange(samples, position, minP, maxP)
                } else {
                    downSampleInput(samples, position, 1)
                    period = findPitchPeriodInRange(downSampleBuffer, 0, minP, maxP)
                }
            }
        }

        retPeriod = if (preferNewPeriod) {
            if (prevPeriod != 0 && prevMinDiff != 0 &&
                prevMinDiff * period <= prevPeriod * prevMinDiff) {
                period
            } else {
                prevPeriod
            }
        } else {
            period
        }

        prevMinDiff = 0
        prevPeriod = period
        return retPeriod
    }

    private fun overlapAdd(
        numSamples: Int,
        numChannels: Int,
        out: ShortArray,
        outPos: Int,
        rampDown: ShortArray,
        rampDownPos: Int,
        rampUp: ShortArray,
        rampUpPos: Int
    ) {
        for (i in 0 until numSamples) {
            val t = numSamples - i
            for (c in 0 until numChannels) {
                val idx = i * numChannels + c
                out[outPos + idx] = ((rampDown[rampDownPos + idx] * t + rampUp[rampUpPos + idx] * i) / numSamples).toShort()
            }
        }
    }

    private fun moveNewSamplesToPitchBuffer(originalNumOutputSamples: Int) {
        val numSamples = numOutputSamples - originalNumOutputSamples
        if (numPitchSamples + numSamples > pitchBufferSize) {
            pitchBufferSize += (pitchBufferSize shr 1) + numSamples
            pitchBuffer = pitchBuffer.copyOf(pitchBufferSize * numChannels)
        }
        System.arraycopy(
            outputBuffer, originalNumOutputSamples * numChannels,
            pitchBuffer, numPitchSamples * numChannels,
            numSamples * numChannels
        )
        numOutputSamples = originalNumOutputSamples
        numPitchSamples += numSamples
    }

    private fun removePitchSamples(numSamples: Int) {
        if (numSamples == 0) return
        System.arraycopy(
            pitchBuffer, numSamples * numChannels,
            pitchBuffer, 0,
            (numPitchSamples - numSamples) * numChannels
        )
        numPitchSamples -= numSamples
    }

    private fun adjustPitch(originalNumOutputSamples: Int) {
        if (pitch == 1.0f) return

        moveNewSamplesToPitchBuffer(originalNumOutputSamples)

        val period: Int
        val newPeriod: Int

        if (pitch >= 1.0f) {
            period = (minPeriod / pitch).toInt().coerceAtLeast(1)
            newPeriod = (period / pitch).toInt().coerceAtLeast(1)
        } else {
            newPeriod = (minPeriod * pitch).toInt().coerceAtLeast(1)
            period = (newPeriod / pitch).toInt().coerceAtLeast(1)
        }

        while (numPitchSamples >= period + maxPeriod) {
            if (pitch >= 1.0f) {
                // 피치 올리기: 샘플 제거
                overlapAdd(
                    period, numChannels,
                    pitchBuffer, 0,
                    pitchBuffer, 0,
                    pitchBuffer, period * numChannels
                )
                removePitchSamples(period)
            } else {
                // 피치 내리기: 샘플 삽입
                enlargeOutputBufferIfNeeded(period)
                System.arraycopy(
                    pitchBuffer, 0,
                    outputBuffer, numOutputSamples * numChannels,
                    period * numChannels
                )
                numOutputSamples += period
                removePitchSamples(newPeriod)
            }
        }

        // 남은 샘플 출력
        if (numPitchSamples > 0) {
            enlargeOutputBufferIfNeeded(numPitchSamples)
            System.arraycopy(
                pitchBuffer, 0,
                outputBuffer, numOutputSamples * numChannels,
                numPitchSamples * numChannels
            )
            numOutputSamples += numPitchSamples
            numPitchSamples = 0
        }
    }

    private fun processStreamInput() {
        val originalNumOutputSamples = numOutputSamples
        val s = speed / pitch

        if (s > 1.00001f || s < 0.99999f) {
            changeSpeed(s)
        } else {
            copyToOutput(inputBuffer, 0, numInputSamples)
            numInputSamples = 0
        }

        if (pitch != 1.0f) {
            adjustPitch(originalNumOutputSamples)
        }
    }

    private fun changeSpeed(speed: Float) {
        if (numInputSamples < maxRequired) return

        val numSamples = numInputSamples
        var position = 0

        do {
            if (remainingInputToCopy > 0) {
                position += copyInputToOutput(position)
            } else {
                val period = findPitchPeriod(inputBuffer, position, false)
                if (speed > 1.0f) {
                    position += (period * (speed - 1.0f)).toInt()
                } else if (speed < 1.0f) {
                    overlapAdd(
                        period, numChannels,
                        outputBuffer, numOutputSamples * numChannels,
                        inputBuffer, position * numChannels,
                        inputBuffer, (position + period) * numChannels
                    )
                    numOutputSamples += period
                    copyToOutput(inputBuffer, position, period)
                    position += (period * speed).toInt()
                } else {
                    position += period
                    copyToOutput(inputBuffer, position, period)
                }
            }
        } while (position + maxRequired <= numSamples)

        removeInputSamples(position)
    }

    fun writeShortsToStream(samples: ShortArray, numSamples: Int) {
        addShortSamplesToInputBuffer(samples, numSamples)
        processStreamInput()
    }

    fun readShortsFromStream(samples: ShortArray, maxSamples: Int): Int {
        val numSamples = if (numOutputSamples > maxSamples) maxSamples else numOutputSamples
        System.arraycopy(
            outputBuffer, 0,
            samples, 0,
            numSamples * numChannels
        )

        // 읽은 샘플 제거
        if (numSamples < numOutputSamples) {
            System.arraycopy(
                outputBuffer, numSamples * numChannels,
                outputBuffer, 0,
                (numOutputSamples - numSamples) * numChannels
            )
        }
        numOutputSamples -= numSamples

        return numSamples
    }

    fun flushStream() {
        val remainingSamples = numInputSamples
        val speed = this.speed
        val pitch = this.pitch

        this.speed = 1.0f
        this.pitch = 1.0f

        if (remainingSamples > 0) {
            copyToOutput(inputBuffer, 0, remainingSamples)
            numInputSamples = 0
        }

        this.speed = speed
        this.pitch = pitch
    }

    companion object {
        private const val SAMPLE_RATE = 44100
    }
}
