package com.example.musicplayer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

class IOSStyleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = 0xFFE0E0E0.toInt() // Light gray for track
    }

    private val arcBounds = RectF()
    private var currentAngle = 0f
    private var isAnimating = false

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isAnimating) {
                currentAngle += 6f
                if (currentAngle >= 360f) {
                    currentAngle = 0f
                }
                invalidate()
                postDelayed(this, 16) // ~60fps
            }
        }
    }

    init {
        // iOS blue color
        paint.color = 0xFF007AFF.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = 100
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredSize, heightSize)
            else -> desiredSize
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        val padding = paint.strokeWidth / 2
        arcBounds.set(
            padding,
            padding,
            size - padding,
            size - padding
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background track
        canvas.drawCircle(
            arcBounds.centerX(),
            arcBounds.centerY(),
            (arcBounds.width() / 2),
            barPaint
        )

        // Draw progress arc (iOS style - partial circle)
        canvas.drawArc(
            arcBounds,
            currentAngle - 90f,
            270f, // 3/4 circle
            false,
            paint
        )
    }

    fun startAnimating() {
        if (!isAnimating) {
            isAnimating = true
            post(animationRunnable)
        }
    }

    fun stopAnimating() {
        isAnimating = false
        removeCallbacks(animationRunnable)
        currentAngle = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimating()
    }

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
        barPaint.strokeWidth = width
        invalidate()
    }
}
