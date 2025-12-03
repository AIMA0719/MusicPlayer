package com.example.musicplayer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 음성 진폭을 원형 이퀄라이저 바로 시각화하는 커스텀 뷰
 * 원의 둘레를 따라 배치된 바들이 진폭에 따라 높이가 변합니다.
 */
class WaveformCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 바(bar) 관련 설정
    private val barCount = 60 // 원형으로 배치할 바의 개수
    private val barHeights = FloatArray(barCount) { 0.2f } // 각 바의 높이 (0 ~ 1)
    private val targetBarHeights = FloatArray(barCount) { 0.2f } // 목표 높이

    // 페인트 객체
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    // 원의 기본 설정
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 100f
    private var maxBarHeight = 50f

    // 진폭 값 (0 ~ 1 정규화)
    private var normalizedAmplitude = 0f

    // 녹음 상태
    private var isRecording = false

    // 애니메이션 업데이트를 위한 Runnable
    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                updateBarHeights()
                invalidate()
                postDelayed(this, 50) // 50ms마다 업데이트
            }
        }
    }

    init {
        // 기본 설정
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val size = min(w, h)
        baseRadius = size * 0.25f
        maxBarHeight = size * 0.15f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 중심 원 그리기
        canvas.drawCircle(centerX, centerY, baseRadius * 0.6f, centerCirclePaint)

        // 원형으로 배치된 바들 그리기
        val angleStep = 360f / barCount

        for (i in 0 until barCount) {
            val angle = i * angleStep - 90 // -90도로 시작 (12시 방향)
            val angleRad = Math.toRadians(angle.toDouble())

            // 바의 높이 (진폭과 랜덤성 결합)
            val barHeight = maxBarHeight * barHeights[i]

            // 바의 시작점 (원의 둘레)
            val startX = centerX + (baseRadius * cos(angleRad)).toFloat()
            val startY = centerY + (baseRadius * sin(angleRad)).toFloat()

            // 바의 끝점 (바깥쪽으로 확장)
            val endX = centerX + ((baseRadius + barHeight) * cos(angleRad)).toFloat()
            val endY = centerY + ((baseRadius + barHeight) * sin(angleRad)).toFloat()

            // 바 그리기
            barPaint.strokeWidth = 8f
            canvas.drawLine(startX, startY, endX, endY, barPaint)
        }
    }

    /**
     * 진폭 값 업데이트
     * @param amplitude MediaRecorder.getMaxAmplitude() 값 (0 ~ 32767)
     */
    fun setAmplitude(amplitude: Int) {
        // 진폭을 0 ~ 1 범위로 정규화
        normalizedAmplitude = (amplitude.toFloat() / 32767f).coerceIn(0f, 1f)

        // 진폭이 너무 작으면 최소값 보장
        if (normalizedAmplitude < 0.1f && isRecording) {
            normalizedAmplitude = 0.1f
        }
    }

    /**
     * 바 높이 업데이트 (부드러운 애니메이션)
     */
    private fun updateBarHeights() {
        for (i in 0 until barCount) {
            // 목표 높이 설정 (진폭 + 랜덤 변화)
            val randomFactor = Random.nextFloat() * 0.3f + 0.7f // 0.7 ~ 1.0
            targetBarHeights[i] = (normalizedAmplitude * randomFactor).coerceIn(0.15f, 1f)

            // 부드럽게 목표 높이로 이동
            val diff = targetBarHeights[i] - barHeights[i]
            barHeights[i] += diff * 0.3f // 30%씩 이동 (부드러운 효과)
        }
    }

    /**
     * 녹음 시작 - 애니메이션 시작
     */
    fun startRecording() {
        isRecording = true
        barPaint.color = Color.parseColor("#F44336") // 빨간색으로 변경
        centerCirclePaint.color = Color.parseColor("#FFCDD2") // 밝은 빨간색

        // 애니메이션 시작
        removeCallbacks(animationRunnable)
        post(animationRunnable)
    }

    /**
     * 녹음 중지 - 애니메이션 중지
     */
    fun stopRecording() {
        isRecording = false
        barPaint.color = Color.parseColor("#4CAF50") // 초록색으로 복원
        centerCirclePaint.color = Color.parseColor("#E0E0E0") // 회색으로 복원

        // 애니메이션 중지
        removeCallbacks(animationRunnable)

        // 모든 바를 최소 높이로 복원
        for (i in 0 until barCount) {
            barHeights[i] = 0.2f
            targetBarHeights[i] = 0.2f
        }
        normalizedAmplitude = 0f
        invalidate()
    }

    /**
     * 일시정지 상태
     */
    fun pauseRecording() {
        isRecording = false
        barPaint.color = Color.parseColor("#FF9800") // 주황색으로 변경
        centerCirclePaint.color = Color.parseColor("#FFE0B2") // 밝은 주황색

        // 애니메이션 중지
        removeCallbacks(animationRunnable)
        invalidate()
    }

    /**
     * 녹음 재개
     */
    fun resumeRecording() {
        isRecording = true
        barPaint.color = Color.parseColor("#F44336") // 빨간색으로 변경
        centerCirclePaint.color = Color.parseColor("#FFCDD2") // 밝은 빨간색

        // 애니메이션 재시작
        removeCallbacks(animationRunnable)
        post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(animationRunnable)
    }
}
