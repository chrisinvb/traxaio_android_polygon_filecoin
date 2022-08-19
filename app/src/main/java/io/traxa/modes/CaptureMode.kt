package io.traxa.modes

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraDeviceCompat
import androidx.core.os.bundleOf
import java.util.concurrent.Executor

abstract class CaptureMode {

    abstract val executor: Executor
    abstract val pictureSurface: Surface?
    private lateinit var cameraCharacteristics: CameraCharacteristicsCompat
    protected lateinit var cameraDeviceCompat: CameraDeviceCompat
    var isReady = false

    /**
     * Cached jpeg orientation value
     */
    protected var jpegOrientation = 0

    /**
     * The physical rotation/orientation of the device.
     * Used to rotate the captured image
     */
    protected var deviceOrientation = 0

    /**
     * Reference to in-use camera
     */
    protected var cameraId: String? = null

    open fun setup(
        cameraId: String,
        cameraCharacteristics: CameraCharacteristicsCompat,
        data: Bundle
    ) {
        this.cameraCharacteristics = cameraCharacteristics
        this.cameraId = cameraId
        isReady = true
    }

    open fun start(
        cameraDeviceCompat: CameraDeviceCompat,
        captureSession: CameraCaptureSessionCompat,
        data: Bundle
    ) {
        deviceOrientation = data.getInt("deviceOrientation")
        jpegOrientation = getJpegOrientationValue()
    }

    abstract fun stop()

    open fun release() {
        isReady = false
    }

    @SuppressLint("RestrictedApi")
    protected fun getJpegOrientationValue(): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Reverse device orientation for front-facing cameras
        val facingFront =
            cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        val deviceOrientationDegrees = when (deviceOrientation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        Log.d("CaptureVideoMode", "deviceOrientation: $deviceOrientationDegrees")
        Log.d("CaptureVideoMode", "sensorOrientation: $sensorOrientation")

        // Reverse device orientation for front-facing cameras
        val sign = if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_FRONT
        ) 1 else -1

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        Log.d("CaptureVideoMode", "jpegOrientation: ${(sensorOrientation - (deviceOrientationDegrees * sign) + 360) % 360}")


        return (sensorOrientation - (deviceOrientationDegrees * sign) + 360) % 360
    }

    abstract fun updateBundle(bundle: Bundle)

    open fun startBundle(deviceOrientation: Int = 0) = bundleOf(
        "deviceOrientation" to deviceOrientation
    )
}