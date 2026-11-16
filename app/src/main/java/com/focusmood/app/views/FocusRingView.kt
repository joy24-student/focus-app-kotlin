package com.focusmood.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class FocusRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f // 0f to 1f
    private var timeText: String = "25:00"
    private var statusText: String = "Ready to focus"
    private var hintText: String = "tap time to customize"

    private var accentColor: Int = Color.parseColor("#10b981")
    private var textColor: Int = Color.parseColor("#f0fdf4")
    private var textMutedColor: Int = Color.parseColor("#86efac")
    private var ringBgColor: Int = Color.parseColor("#22ffffff")

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val oval = RectF()

    fun updateThemeColors(accent: Int, text: Int, muted: Int) {
        this.accentColor = accent
        this.textColor = text
        this.textMutedColor = muted
        invalidate()
    }

    fun setProgress(progress: Float, timeFormatted: String, status: String) {
        this.progress = progress.coerceIn(0f, 1f)
        this.timeText = timeFormatted
        this.statusText = status
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        val size = Math.min(width, height)
        val strokeWidth = size * 0.055f

        bgPaint.strokeWidth = strokeWidth
        bgPaint.color = ringBgColor

        progressPaint.strokeWidth = strokeWidth
        progressPaint.color = accentColor

        val padding = strokeWidth / 2f + 16f
        oval.set(
            (width - size) / 2f + padding,
            (height - size) / 2f + padding,
            (width + size) / 2f - padding,
            (height + size) / 2f - padding
        )

        // 1. Draw Ring Background Circle
        canvas.drawOval(oval, bgPaint)

        // 2. Draw Progress Arc (-90 degrees is top)
        val sweepAngle = progress * 360f
        if (sweepAngle > 0.5f) {
            canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
        }

        // 3. Draw Center Time Text
        val centerX = width / 2f
        val centerY = height / 2f

        timePaint.color = textColor
        timePaint.textSize = size * 0.22f
        val timeBaseline = centerY + (timePaint.textSize * 0.15f)
        canvas.drawText(timeText, centerX, timeBaseline, timePaint)

        // 4. Draw Status Subtitle Text
        statusPaint.color = textMutedColor
        statusPaint.textSize = size * 0.058f
        val statusBaseline = timeBaseline + (size * 0.12f)
        canvas.drawText(statusText, centerX, statusBaseline, statusPaint)

        // 5. Draw Tap Hint
        hintPaint.color = Color.argb(120, Color.red(textMutedColor), Color.green(textMutedColor), Color.blue(textMutedColor))
        hintPaint.textSize = size * 0.042f
        val hintBaseline = statusBaseline + (size * 0.09f)
        canvas.drawText(hintText, centerX, hintBaseline, hintPaint)
    }
}
