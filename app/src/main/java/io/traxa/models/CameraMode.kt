package io.traxa.models

import io.traxa.R

/**
 * Data class that represents a camera mode.
 * The selected CameraMode will change how the app works when the
 * record button is pressed.
 *
 * @param name Name of the camera mode
 * @param type Type of the camera mode (VIDEO, OCR)
 *
 */
data class CameraMode(
    val name: String,
    val type: Type
) {

    companion object {
        const val VIDEO = 1
    }

    sealed class Type {
        abstract val name: Int
        open class Video(override val name: Int = R.string.manual_video) : Type()
    }

}