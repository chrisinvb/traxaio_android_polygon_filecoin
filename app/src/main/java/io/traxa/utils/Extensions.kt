package io.traxa.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.ScaleAnimation
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.io.File

//Listen device orientation changes from an Activity
fun Activity.listenOrientationChanges(onOrientationChanged: ((Int) -> (Unit))) {
    val orientationEventListener = object : OrientationEventListener(this) {
        override fun onOrientationChanged(orientation: Int) {

            val rotation = when {
                orientation <= 45 -> Surface.ROTATION_0
                orientation <= 135 -> Surface.ROTATION_90
                orientation <= 225 -> Surface.ROTATION_180
                orientation <= 315 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }

            onOrientationChanged(rotation)
        }

    }
    orientationEventListener.enable()
}

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.dpF: Float
    get() = this / Resources.getSystem().displayMetrics.density

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxF: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

fun View.jumpZ(duration: Long = 300) {
    val jump = ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f, width / 2f, height / 2f)
    jump.duration = duration
    jump.interpolator = CycleInterpolator(1F)

    this.startAnimation(jump)
}

fun Context.showMap(lat: Double, lon: Double) {
    val uri = "geo:%f,%f".format(lat, lon)
    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
    val browser = Intent.parseUri(mapsUrl, 0)

    if (browser.resolveActivity(packageManager) != null) {
        startActivity(browser)
    } else {
        val gmaps = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        gmaps.setPackage("com.google.android.apps.maps")
        if (gmaps.resolveActivity(packageManager) != null) {
            startActivity(gmaps)
        } else {
            val general = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            if (general.resolveActivity(packageManager) != null) {
                startActivity(general)
            } else startActivity(browser)
        }
    }
}

/**
 * Combine two livedata
 */
fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}


val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024