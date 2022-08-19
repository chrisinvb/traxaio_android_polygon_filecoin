package io.traxa.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Returns formatted date string from a timestamp
 */
@SuppressLint("SimpleDateFormat")
fun dateString(timeStamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd'T'hhmmssSSS'Z'").also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

    return dateFormat.format(Date(timeStamp))
}