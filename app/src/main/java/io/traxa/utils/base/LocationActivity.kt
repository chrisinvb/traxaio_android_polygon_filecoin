package io.traxa.utils.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.traxa.services.LocationService

/**
 * Base class for activities that wish to use a [LocationService]
 *
 **/
open class LocationActivity : AppCompatActivity() {

    /**
     * Location service used to get location updates
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val locationService by lazy { LocationService(this) }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
        }

        locationService.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        locationService.stopLocationUpdates()
    }

}