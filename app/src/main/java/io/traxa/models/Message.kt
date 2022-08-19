package io.traxa.models
import io.traxa.R

/**
 * Data class that represents a Message, typically displayed on the main screen.
 *
 * @param text Message text
 * @param type Message type (WARNING, ERROR, ecc...)
 * @param action Action to perform when clicked
 *
 */
data class Message(
    val text: String,
    val type: Type,
    val action: (() -> Unit)? = null
) {
    enum class Type {
        WARNING, ERROR, INFO, UPLOAD_INFO, NONE, GPS_WARNING
    }

    //Select best icon from message type
    fun icon() = when (type) {
        Type.WARNING -> R.drawable.ic_baseline_warning_24
        Type.GPS_WARNING -> R.drawable.ic_baseline_not_listed_location_24
        Type.ERROR -> R.drawable.ic_baseline_error_outline_24
        Type.INFO -> R.drawable.ic_baseline_info_24
        Type.UPLOAD_INFO -> R.drawable.ic_baseline_cloud_upload_24
        Type.NONE -> -1
    }
}