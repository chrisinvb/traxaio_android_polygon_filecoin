package io.traxa.services

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import dispatch.core.MainCoroutineScope
import io.traxa.models.CameraMode
import io.traxa.modes.CaptureMode
import io.traxa.modes.CaptureModeProcessor
import io.traxa.modes.video.CaptureVideoMode
import io.traxa.modes.video.CaptureVideoModeProcessor
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Service used to capture videos/photos and upload
 * them in AWS.
 *
 * @param context Fragment/Activity context
 * @param cameraService Initialized [CameraService] used to easily interface with the camera
 */
class RecordingService(
    val context: Context,
    private val cameraService: CameraService
) : KoinComponent {

    private var captureVideoMode = CaptureVideoMode(cameraService.previewView)
    private var captureVideoModeProcessor = CaptureVideoModeProcessor(context)

    private var captureModeProcessor: CaptureModeProcessor = captureVideoModeProcessor
    var captureMode: CaptureMode = captureVideoMode.also {
        cameraService.captureMode = captureVideoMode
    }

    private var cameraSelector: CameraSelector? = null
    private lateinit var cameraMode: CameraMode.Type

    var latestRecordingId: Int = -1

    /**
     * Device orientation used to rotate
     * videos and pictures
     */
    var deviceOrientation: Int = 0

    /**
     * Room database
     */
    private val database: AppDatabase by inject()
    private val recordingDao = database.recordingDao()

    private var location: LiveData<Location>? = null
    private var lifecycleOwner: LifecycleOwner? = null

    fun init(deviceOrientation: Int, cameraSelector: CameraSelector, cameraMode: CameraMode.Type) {
        var setupBundle: Bundle = bundleOf()
        this.deviceOrientation = deviceOrientation
        this.cameraSelector = cameraSelector
        this.cameraMode = cameraMode

        when (cameraMode) {
            is CameraMode.Type.Video -> {
                captureModeProcessor = captureVideoModeProcessor
                captureMode = captureVideoMode
                setupBundle = captureVideoMode.setupBundle(deviceOrientation)
            }
        }

        cameraService.captureMode = captureMode
        cameraService.startCamera(cameraSelector, setupBundle)
    }

    fun startRecordingWithLiveLocation(
        location: LiveData<Location>,
        targetFPS: Double,
        deviceOrientation: Int,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        this.location = location
        this.lifecycleOwner = lifecycleOwner

        captureModeProcessor.location = location
        CoroutineScope(Dispatchers.Main).launch {

            //Create recording entity in database
            val recording = Recording(cameraMode)
            val id = withContext(Dispatchers.IO) {
                recordingDao.insertAll(recording).first()
            }

            latestRecordingId = id.toInt()
            val startBundle = getStartBundle(targetFPS, deviceOrientation)

            cameraService.startCapture(startBundle)
            captureModeProcessor.start(captureMode, latestRecordingId)
        }
    }

    fun stopRecording() {
        MainCoroutineScope().launch {
            captureModeProcessor.stop(captureMode)
            cameraService.stopCapture()
        }

        if (lifecycleOwner != null) location?.removeObservers(lifecycleOwner!!)
    }

    fun processRecording() = MainCoroutineScope().launch {
        captureModeProcessor.process()
    }

    private fun getStartBundle(targetFPS: Double,
                               deviceOrientation: Int): Bundle = when (cameraMode) {
        is CameraMode.Type.Video -> captureVideoMode.startBundle(deviceOrientation)
    }

}