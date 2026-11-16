package com.focusmood.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class EqualizerBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isPlaying = false
    private var barColor = Color.parseColor("#10b981")
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barHeights = floatArrayOf(0.3f, 0.7f, 0.4f, 0.8f)
    private var animator: ValueAnimator? = null
    private val rect = RectF()

    fun setBarColor(color: Int) {
        this.barColor = color
        invalidate()
    }

    fun setPlaying(playing: Boolean) {
        if (this.isPlaying == playing) return
        this.isPlaying = playing

        if (playing) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        stopAnimation()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                barHeights[0] = 0.2f + 0.6f * Math.abs(Math.sin((v * Math.PI)).toFloat())
                barHeights[1] = 0.3f + 0.7f * Math.abs(Math.cos((v * Math.PI)).toFloat())
                barHeights[2] = 0.2f + 0.8f * Math.abs(Math.sin((v * Math.PI + 0.5)).toFloat())
                barHeights[3] = 0.3f + 0.6f * Math.abs(Math.cos((v * Math.PI + 0.5)).toFloat())
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        barHeights[0] = 0.25f
        barHeights[1] = 0.35f
        barHeights[2] = 0.25f
        barHeights[3] = 0.35f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        barPaint.color = barColor

        val barCount = 4
        val spacing = w * 0.12f
        val barWidth = (w - (spacing * (barCount - 1))) / barCount

        for (i in 0 until barCount) {
            val left = i * (barWidth + spacing)
            val right = left + barWidth
            val barH = h * barHeights[i]
            val top = h - barH
            val bottom = h

            rect.set(left, top, right, bottom)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
