package io.traxa.ui.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import io.traxa.R
import io.traxa.utils.pxF

class Pulser @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    private lateinit var colorAnimator: ValueAnimator
    private var dropRadiusAnimator: ValueAnimator? = null

    private var drawPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        strokeWidth = 10F
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND

        isFilterBitmap = true
        isDither = true
    }

    private var startCircleColor: Int = ContextCompat.getColor(context, android.R.color.white)
    private var dropCircleColor: Int = ContextCompat.getColor(context, R.color.black)
    private var circleColor = startCircleColor

    var circleRadius = 32.pxF
    private var dropRadius = circleRadius + 10.pxF
    var dropDirection = 1
    var switchDirections = false
    private var dropAlpha = 0F
    private var maxDropRadius = circleRadius + 84.pxF

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (maxDropRadius * 2).toInt()
        val desiredHeight = (maxDropRadius * 2).toInt()
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        //Measure Width
        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }

        //Measure Height
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        drawPaint.color = dropCircleColor
        drawPaint.alpha = dropAlpha.toInt()
        canvas.drawCircle(maxDropRadius, maxDropRadius, dropRadius, drawPaint)

        drawPaint.color = circleColor
        drawPaint.alpha = 255
        canvas.drawCircle(maxDropRadius, maxDropRadius, circleRadius, drawPaint)

    }

    fun start() {
        Handler(Looper.getMainLooper()).post { drop() }
    }

    fun stop() {
        dropRadiusAnimator?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    fun drop() {
        if (dropRadiusAnimator == null) {
            if(switchDirections) dropDirection *= -1

            dropRadiusAnimator = if(dropDirection == -1) ValueAnimator.ofFloat(maxDropRadius, 0F)
            else ValueAnimator.ofFloat(0F, maxDropRadius)

            dropRadiusAnimator!!.duration = 2000
            dropRadiusAnimator!!.repeatCount = 1
            dropRadiusAnimator!!.doOnEnd {
                if(isShown) {
                    Handler(Looper.getMainLooper()).postDelayed({ drop() },
                        (1000 + (1000 * Math.random())).toLong()
                    )
                }
            }
            dropRadiusAnimator!!.addUpdateListener {
                dropRadius = it.animatedValue as Float
                dropAlpha = (1 - (dropRadius / maxDropRadius)) * 255
                invalidate()
            }

        } else if (dropRadiusAnimator!!.isRunning) {
//            dropRadiusAnimator!!.cancel()
//            ValueAnimator.ofFloat(dropAlpha, 100F).apply {
//                duration = 150
//                repeatCount = 1
//                addUpdateListener { invalidate() }
//                doOnEnd { dropRadiusAnimator?.start() }
//            }.start()
        }

        dropRadiusAnimator?.start()
    }
}