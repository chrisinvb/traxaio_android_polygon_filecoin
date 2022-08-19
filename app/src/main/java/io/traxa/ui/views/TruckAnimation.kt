package io.traxa.ui.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import io.traxa.R
import io.traxa.utils.pxF

class TruckAnimation @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    inner class Truck(var x: Float, var color: Int)

    private var truckAnimator: ValueAnimator? = null

    private val truck = ContextCompat.getDrawable(context, R.drawable.ic_truck)
        .let { (it as VectorDrawable).toBitmap() }

    private val colors = arrayOf(
        R.color.blue,
        R.color.red,
        R.color.green,
        R.color.yellow,
        R.color.pink,
        R.color.purple_500
    )

    private val trucks = arrayListOf<Truck>()

    private var truckSpeed = 1f
    private val roadHeight = 10.pxF
    private val roadColor = ContextCompat.getColor(context, R.color.gray)
    private val truckPadding = 50.pxF
    private var running = false

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

    private fun randomColorFilter() = PorterDuffColorFilter(
        ContextCompat.getColor(context, colors.random()),
        PorterDuff.Mode.MULTIPLY
    )

    private fun colorFilter(color: Int) = PorterDuffColorFilter(
        ContextCompat.getColor(context, color),
        PorterDuff.Mode.MULTIPLY
    )

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        for (i in (0 until (width / truck.width))) {
            if (trucks.size < 4)
                trucks.add(Truck(i * (truck.width + truckPadding), colors.random()))
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        drawPaint.colorFilter = null
        drawPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width * 1f, height * 1f, drawPaint)

        drawPaint.color = roadColor
        canvas.drawRect(0f, height - roadHeight, width * 1f, height * 1f, drawPaint)

        trucks.forEach {
            drawPaint.colorFilter = colorFilter(it.color)

            if (it.x > width) {
                it.x = -truck.width - truckPadding * 1f
                it.color = colors.random()
            }

            canvas.drawBitmap(truck, it.x, (height - truck.height - roadHeight) * 1f, drawPaint)

            it.x += truckSpeed
        }

        if (running) invalidate()
    }

    fun start() {
        running = true
        invalidate()
    }

    fun stop() {
        running = false
    }
}