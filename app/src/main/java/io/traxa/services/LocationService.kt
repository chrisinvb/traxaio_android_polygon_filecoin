package io.traxa.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.fondesa.kpermissions.extension.checkPermissionsStatus
import com.fondesa.kpermissions.isGranted
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import io.traxa.R
import io.traxa.models.Message


/**
 * Location service class that uses FusedLocationProvider as default
 * and normal LocationManager as fallback
 */
class LocationService(private val activity: Activity) {

    data class LocationStatus(val type: StatusType)
    enum class StatusType {
        LOCATION_OK,
        NO_GPS,
        WAITING_LOCATION
    }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val turnOnGpsMessage = Message(
        "Turn GPS on",
        Message.Type.GPS_WARNING
    ) { showEnableLocationSetting() }

    private val locationAvailable = Message(
        activity.getString(R.string.waiting_location),
        Message.Type.GPS_WARNING, null
    )

    //-------------------- GPS settings --------------------

    private val locationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)

    private val client: SettingsClient = LocationServices.getSettingsClient(activity)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

    //------------------- end GPS settings -------------------

    var locationUpdateCallback: ((List<Location>) -> Unit)? = null
    var messageCallback: ((Message) -> Unit)? = null
    var statusCallback: ((LocationStatus) -> Unit)? = null

    /**
     * Used to listen for android gps status (enabled/disabled)
     */
    private val mGpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {

                val locationManager =
                    context!!.getSystemService(LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGpsEnabled || isNetworkEnabled) {
                    statusCallback?.invoke(LocationStatus(StatusType.WAITING_LOCATION))
                    messageCallback?.invoke(locationAvailable)
                } else {
                    statusCallback?.invoke(LocationStatus(StatusType.NO_GPS))
                    messageCallback?.invoke(turnOnGpsMessage)
                }
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationUpdateCallback?.invoke(locationResult.locations)
            statusCallback?.invoke(LocationStatus(StatusType.LOCATION_OK))
        }

        override fun onLocationAvailability(availability: LocationAvailability) = Unit
    }

    private val locationListener = object : LocationListener {
        override fun onProviderEnabled(provider: String) = Unit
        override fun onLocationChanged(location: Location) {
            locationUpdateCallback?.invoke(listOf(location))
            statusCallback?.invoke(LocationStatus(StatusType.LOCATION_OK))

        }

        override fun onProviderDisabled(provider: String) {
            statusCallback?.invoke(LocationStatus(StatusType.NO_GPS))
            messageCallback?.invoke(turnOnGpsMessage)
        }
    }

    private fun isGooglePlayServicesAvailable() = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(activity) == ConnectionResult.SUCCESS


    private fun isAllPermissionsGranted() = activity.checkPermissionsStatus(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).map { it.isGranted() }.reduce { a, b -> a && b }


    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        activity.registerReceiver(
            mGpsSwitchStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )

        if (!gpsStatus) {
            messageCallback?.invoke(turnOnGpsMessage)
            statusCallback?.invoke(LocationStatus(StatusType.NO_GPS))
        } else if (isAllPermissionsGranted()) {
            messageCallback?.invoke(locationAvailable)
            if (isGooglePlayServicesAvailable()) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } else locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0F,
                locationListener
            )
        } else throw Exception("Missing permissions!")
    }

    private fun showEnableLocationSetting() {
        if (isGooglePlayServicesAvailable()) {
            val task = client.checkLocationSettings(builder.build())

            task.addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        Log.e("LocationService", "Failure")

                        //Handle result in onActivityResult()
                        e.startResolutionForResult(activity, 100)
                    } catch (sendEx: IntentSender.SendIntentException) {
                    }
                } else activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100) {
            when (resultCode) {
                Activity.RESULT_OK -> startLocationUpdates()
                Activity.RESULT_CANCELED -> activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    fun stopLocationUpdates() {
        try {
            activity.unregisterReceiver(mGpsSwitchStateReceiver)
        } catch (e: IllegalArgumentException) {
            //Receiver was already unregistered
        }

        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } else {
            val locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(locationListener)
        }
    }

}