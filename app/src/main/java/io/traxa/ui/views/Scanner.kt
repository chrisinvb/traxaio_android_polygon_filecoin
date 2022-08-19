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
import android.view.animation.PathInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import io.traxa.R
import io.traxa.utils.pxF

class Scanner @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    private var lineAnimator: ValueAnimator? = null

    private var drawPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        strokeWidth = 5F
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND

        isFilterBitmap = true
        isDither = true
    }

    private var startCircleColor: Int = ContextCompat.getColor(context, android.R.color.white)

    var width = 256.pxF
    var height = (width * 6f/3f)

    var lineYStart = 10F
    var lineY = lineYStart
    var startX = 10F
    var startY = 10F

    var latestDirection = -1

    //var squareColor = Color.parseColor("#dddbdb")
    var squareColor = Color.parseColor("#ffffff")
    var lineColor = Color.YELLOW

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (width + 4.pxF).toInt()
        val desiredHeight = (height + 12.pxF).toInt()

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
        drawPaint.color = lineColor
        drawPaint.strokeWidth = 5.pxF
        drawPaint.style = Paint.Style.FILL_AND_STROKE
        canvas.drawLine(startX, lineY, width, lineY, drawPaint)

        drawPaint.color = squareColor
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeWidth = 5.pxF
        canvas.drawRoundRect(
            startX, startY, width, height, 12F, 12F, drawPaint)
    }

    fun start() {
        Handler(Looper.getMainLooper()).post { scan() }
    }

    fun stop() {
        lineAnimator?.cancel()
    }

    fun scan() {
        if (lineAnimator == null) {

            lineAnimator = if(latestDirection == -1) ValueAnimator.ofFloat(lineYStart, height)
            else ValueAnimator.ofFloat(height, lineYStart)

            lineAnimator!!.duration = 1500
            lineAnimator!!.repeatCount = 1
            lineAnimator!!.interpolator = PathInterpolator(.98F,.01F,.01F,1F)
            lineAnimator!!.doOnEnd {
                if(isShown) {
                    Handler(Looper.getMainLooper()).postDelayed({ scan() }, 1500L)
                }
            }

            lineAnimator!!.addUpdateListener {
                lineY = it.animatedValue as Float
                invalidate()
            }

        }

        lineAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}