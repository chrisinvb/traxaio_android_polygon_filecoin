package io.traxa.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraDeviceCompat
import io.traxa.utils.CompareSizesByArea
import io.traxa.utils.EmptyCaptureCallback
import java.util.*
import java.util.concurrent.Executor

class CameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AutoFitTextureView(context, attrs, defStyleAttr) {

    val surface: Surface by lazy { Surface(surfaceTexture) }

    /**
     * Preview resolution
     */
    private var previewSize: Size? = null

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_WIDTH = 1280

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_HEIGHT = 720

    /**
     * Function called when the camera preview is ready
     */
    var onCameraPreviewReady: (() -> Unit)? = null

    /**
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     * class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()

        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = ArrayList()
        val w: Int = aspectRatio.width
        val h: Int = aspectRatio.height
        for (option in choices)
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w)
                if (option.width >= textureViewWidth && option.height >= textureViewHeight)
                    bigEnough.add(option) else notBigEnough.add(option)

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return when {
            bigEnough.isNotEmpty() -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.isNotEmpty() -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> choices[0]
        }
    }

    @SuppressLint("RestrictedApi")
    fun setPreviewAspectRatio(cameraCharacteristics: CameraCharacteristicsCompat) {
        val sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        val swappedDimensions = sensorOrientation == 90 || sensorOrientation == 270

        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val outputSizes = map.getOutputSizes(ImageFormat.JPEG)

        //Largest available preview size
        val largest = Collections.max(
            outputSizes.toList(),
            CompareSizesByArea()
        )

        val displaySize = Point()
        var rotatedPreviewWidth: Int = width
        var rotatedPreviewHeight: Int = height
        var maxPreviewWidth: Int = displaySize.x
        var maxPreviewHeight: Int = displaySize.y

        if (swappedDimensions) {
            rotatedPreviewWidth = height
            rotatedPreviewHeight = width
            maxPreviewWidth = displaySize.y
            maxPreviewHeight = displaySize.x
        }


        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

        // Attempting to use too large a preview size could  exceed the camera
        // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
        // garbage capture data.
        previewSize = chooseOptimalSize(
            outputSizes,
            rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
            maxPreviewHeight, largest
        )

        // Fit the aspect ratio of TextureView to the size of preview we picked.
        val orientation: Int = context.resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            setAspectRatio(previewSize!!.width, previewSize!!.height)
        else
            setAspectRatio(previewSize!!.height, previewSize!!.width)

    }

    @SuppressLint("RestrictedApi")
    fun startPreview(
        cameraDevice: CameraDeviceCompat,
        captureSession: CameraCaptureSessionCompat,
        executor: Executor
    ) {

        try {
            //Stop any previous repeating requests (startPreviews acts as a restart)
            captureSession.toCameraCaptureSession().stopRepeating()

            //Start preview capture
            val captureRequest = cameraDevice.toCameraDevice()
                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            captureRequest.addTarget(surface)

            captureSession.toCameraCaptureSession().stopRepeating()
            captureSession.setSingleRepeatingRequest(
                captureRequest.build(),
                executor,
                EmptyCaptureCallback()
            )

            onCameraPreviewReady?.invoke()
        } catch (e: Exception) {
//            MaterialAlertDialogBuilder(context)
//                .setTitle(R.string.camera_not_supported)
//                .setMessage(R.string.unsupported_camera_explainer)
//                .setCancelable(false)
//                .setPositiveButton(R.string.close) { _, _ -> exitProcess(0) }
//                .show()
        }
    }

    fun stopPreview() {
        //surface.release()
    }

}