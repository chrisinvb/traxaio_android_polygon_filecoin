package io.traxa.utils

import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

class RecyclerBounceOverscroll {

    private val OVERSCROLL_TRANSLATION_MAGNITUDE = 0.2f
    private val FLING_TRANSLATION_MAGNITUDE = 0.5f
    private var translationY: SpringAnimation? = null

    fun attach(recyclerView: RecyclerView) {

        translationY = SpringAnimation(recyclerView, SpringAnimation.TRANSLATION_Y)
            .setSpring(
                SpringForce()
                    .setFinalPosition(0f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                    .setStiffness(SpringForce.STIFFNESS_LOW)
            )

        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {
                return object : EdgeEffect(recyclerView.context) {

                    override fun onPull(deltaDistance: Float) = handlePull(deltaDistance)
                    override fun onPull(deltaDistance: Float, displacement: Float) = handlePull(deltaDistance)
                    override fun onRelease() = translationY!!.start()

                    private fun handlePull(deltaDistance: Float) {
                        val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                        val translationYDelta =
                            sign * recyclerView.width * deltaDistance * OVERSCROLL_TRANSLATION_MAGNITUDE

                        translationY?.cancel()
                        recyclerView.translationY += translationYDelta
                    }

                    override fun onAbsorb(velocity: Int) {
                        val sign = if (direction == DIRECTION_BOTTOM) -1 else 1

                        val translationVelocity = sign * velocity * FLING_TRANSLATION_MAGNITUDE
                        translationY?.setStartVelocity(translationVelocity)?.start()
                    }
                }
            }
        }

    }
}