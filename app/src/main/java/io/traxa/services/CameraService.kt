package io.traxa.services
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraDeviceCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat
import androidx.camera.core.CameraSelector
import io.traxa.modes.CaptureMode
import io.traxa.modes.video.CaptureVideoMode
import io.traxa.ui.views.CameraPreviewView
import org.koin.core.component.KoinComponent
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


/**
 * A class that helps to work with the camera(taking pictures, configurations, videos)
 *
 */
class CameraService(
    private val context: Context,
    val previewView: CameraPreviewView,
    var captureMode: CaptureMode = CaptureVideoMode(previewView)
) : KoinComponent {
    //private val logService: LogService by inject()

    //Camera related
    private var cameraCharacteristics: CameraCharacteristicsCompat? = null
    private var captureSession: CameraCaptureSessionCompat? = null
    private var cameraDevice: CameraDeviceCompat? = null
    private var cameraId: String? = null

    //Threads and background executors
    private var cameraBackgroundExecutor = Executors.newCachedThreadPool()

    private fun pictureSurface() = captureMode.pictureSurface!!


    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * Called when a new capture session is created
     */
    var onCameraConfigured: (() -> Unit)? = null

    private val tag = "CameraService"

    /**
     * Stop camera, close and release surfaces
     */
    @SuppressLint("RestrictedApi")
    fun stop() {
        try {
            mCameraOpenCloseLock.acquire()
            if (captureSession != null) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                    captureSession?.toCameraCaptureSession()?.stopRepeating()
                    captureSession?.toCameraCaptureSession()?.abortCaptures()
                    SystemClock.sleep(500)
                }

                captureSession!!.toCameraCaptureSession().close()
                captureSession = null
            }

            if (cameraDevice != null) {
                cameraDevice!!.toCameraDevice().close()
                cameraDevice = null
            }

            previewView.stopPreview()
            captureMode.release()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Checks if camera is correctly initialized
     *
     * @return Whether the camera is ready
     */
    fun isCameraReady() = captureSession != null

    /**
     * Get [CameraCharacteristicsCompat] for selected camera
     *
     * @param cameraSelector Selected camera
     * @return camera id
     */
    @SuppressLint("RestrictedApi")
    private fun setupCameraCharacteristics(cameraSelector: CameraSelector): String {
        val cameraManager = CameraManagerCompat.from(context)

        var cameraId: String? = null
        cameraCharacteristics = null

        for (camera in cameraManager.cameraIdList) {
            cameraCharacteristics = cameraManager.getCameraCharacteristicsCompat(camera)
            val facing = cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING)

            if (facing != null && facing == cameraSelector.lensFacing) {
                cameraId = camera
                break
            }
        }

        if (cameraId == null) {
            Log.e(tag, "Unable to find camera with configured LENS_FACING")
            return setupCameraCharacteristics(CameraSelector.DEFAULT_BACK_CAMERA)
        }

        return cameraId
    }

    /**
     * Select and initialize the camera, then start the preview
     */
    @SuppressLint("UnsafeExperimentalUsageError", "MissingPermission", "RestrictedApi")
    fun startCamera(
        cameraSelector: CameraSelector,
        data: Bundle
    ) {
        stop()
        cameraId = setupCameraCharacteristics(cameraSelector)
        captureMode.setup(cameraId!!, cameraCharacteristics!!, data)

        previewView.setPreviewAspectRatio(cameraCharacteristics!!)
        openCamera(cameraId!!) {
            createCaptureSession()
        }
    }

    fun updateBundle(data: Bundle) = captureMode.updateBundle(data)

    fun startCapture(data: Bundle) {
        try {
            captureMode.start(cameraDevice!!, captureSession!!, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopCapture() {
        if (captureMode.isReady) {
            captureMode.stop()
            createCaptureSession()
        }
    }

    @SuppressLint("RestrictedApi", "MissingPermission")
    private fun openCamera(cameraId: String,
                           onOpened: ((cd: CameraDeviceCompat) -> Unit)
    ) {
        val cameraManager = CameraManagerCompat.from(context)

        mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)
        cameraManager.openCamera(
            cameraId,
            cameraBackgroundExecutor,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraOpenCloseLock.release()
                    val compat = CameraDeviceCompat.toCameraDeviceCompat(camera)

                    cameraDevice = compat
                    onOpened(compat)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    mCameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    Log.e(tag, "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    mCameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null

                    //logService.d("CameraService", "Camera error number $error")
                    Log.e(tag, "Camera error number $error")
                }

            })
    }

    @SuppressLint("RestrictedApi", "MissingPermission")
    private fun createCaptureSession() {
        val previewSurface = OutputConfigurationCompat(previewView.surface)
        val imageSurface = OutputConfigurationCompat(pictureSurface())

        if(captureSession != null) {
            captureSession!!.toCameraCaptureSession().close()
            captureSession = null
        }

        val configuration = SessionConfigurationCompat(
            SessionConfigurationCompat.SESSION_REGULAR,
            listOf(previewSurface, imageSurface),
            cameraBackgroundExecutor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    captureSession = CameraCaptureSessionCompat
                        .toCameraCaptureSessionCompat(cameraCaptureSession)

                    previewView.startPreview(
                        cameraDevice!!,
                        captureSession!!,
                        cameraBackgroundExecutor
                    )

                    onCameraConfigured?.invoke()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Log.e(tag, "CONFIGURATION FAILED!")
                    //logService.d("CameraService", "Camera configuration failed")
                }
            }
        )

        try {
            cameraDevice!!.createCaptureSession(configuration)
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
}