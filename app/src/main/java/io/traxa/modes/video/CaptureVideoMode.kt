package io.traxa.modes.video

import android.annotation.SuppressLint
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraDeviceCompat
import io.traxa.modes.CaptureMode
import io.traxa.ui.views.CameraPreviewView
import io.traxa.utils.EmptyCaptureCallback
import kotlinx.coroutines.Job
import org.koin.core.component.KoinComponent
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class CaptureVideoMode(
    private val previewView: CameraPreviewView
) : CaptureMode(), KoinComponent {

    override val pictureSurface: Surface?
        get() = mediaRecorder?.surface

    private var mediaRecorder: MediaRecorder? = null

    private var outputFile: File? = null
    private val videoFile = File(previewView.context.getExternalFilesDir("captures"), "video.mp4")

    private var _profile: CamcorderProfile? = null
    private fun profile(cameraId: Int) =
        when {
            _profile != null -> _profile!!

            CamcorderProfile.hasProfile(
                cameraId,
                CamcorderProfile.QUALITY_1080P
            ) -> CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)

            CamcorderProfile.hasProfile(
                cameraId,
                CamcorderProfile.QUALITY_HIGH
            ) -> CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)

            CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_4KDCI) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CamcorderProfile.get(
                    CamcorderProfile.QUALITY_4KDCI
                )
                else CamcorderProfile.get(CamcorderProfile.QUALITY_720P)

            CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QHD) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CamcorderProfile.get(
                    CamcorderProfile.QUALITY_QHD
                )
                else CamcorderProfile.get(CamcorderProfile.QUALITY_720P)

            CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_2K) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CamcorderProfile.get(
                    CamcorderProfile.QUALITY_2K
                )
                else CamcorderProfile.get(CamcorderProfile.QUALITY_720P)

            else -> CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
        }.also {
            if (it.videoFrameRate > 30) it.videoFrameRate = 30
            _profile = it
        }


    /** Creates a [MediaRecorder] instance */
    private fun createRecorder(profile: CamcorderProfile) = MediaRecorder().apply {
        outputFile = File(previewView.context.getExternalFilesDir("captures"), "${System.currentTimeMillis()}.mp4")
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        setVideoFrameRate(profile.videoFrameRate)
        setOutputFile(outputFile!!.absolutePath)
        setVideoEncodingBitRate(profile.videoBitRate)
        setOrientationHint(jpegOrientation)

        prepare()
    }

    override val executor: ExecutorService = Executors.newCachedThreadPool()
    private var videoJob: Job? = null

    override fun setup(
        cameraId: String,
        cameraCharacteristics: CameraCharacteristicsCompat,
        data: Bundle
    ) {
        super.setup(cameraId, cameraCharacteristics, data)

        deviceOrientation = data.getInt("deviceOrientation")
        jpegOrientation = getJpegOrientationValue()

        Log.d("CaptureVideoMode", "Creating camera recorder")
        mediaRecorder = createRecorder(profile(cameraId.toInt()))
    }

    /**
     * Start a job that takes photos continuously.
     * It implements a sort of game loop by measuring the time taken to
     * take a burst picture and then waiting
     *
     */
    override fun start(
        cameraDeviceCompat: CameraDeviceCompat,
        captureSession: CameraCaptureSessionCompat,
        data: Bundle
    ) {
        super.start(cameraDeviceCompat, captureSession, data)
        startVideoRecord(captureSession)
    }

    /**
     * Stop the recording running in [videoJob]
     */
    override fun stop() {
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()

        outputFile?.renameTo(videoFile)
        mediaRecorder = createRecorder(_profile ?: profile(cameraId!!.toInt()))
    }


    /**
     * Start a video recording
     *
     * @param captureSession Camera capture session
     */
    @SuppressLint("RestrictedApi")
    private fun startVideoRecord(captureSession: CameraCaptureSessionCompat) {
        val cameraDevice = captureSession.toCameraCaptureSession().device
        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureSession.setSingleRepeatingRequest(
            recordRequest(cameraDevice),
            executor,
            EmptyCaptureCallback()
        )

        mediaRecorder?.start()
    }

    /**
     * Requests used for preview and recording in the [CameraCaptureSessionCompat]
     **/
    private fun recordRequest(cameraDevice: CameraDevice): CaptureRequest {
        // Capture request holds references to target surfaces
        return cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {

            //Rotate image
            set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)

            // Add the preview and recording surface targets
            addTarget(previewView.surface)
            addTarget(mediaRecorder!!.surface)

            // Sets user requested FPS for all targets
            set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(_profile!!.videoFrameRate, _profile!!.videoFrameRate)
            )
        }.build()
    }

    override fun release() {
        if (mediaRecorder != null) {
            mediaRecorder!!.release()
        }

        super.release()
    }

    fun setupBundle(deviceOrientation: Int): Bundle = startBundle(deviceOrientation)

    override fun updateBundle(bundle: Bundle) {}
}