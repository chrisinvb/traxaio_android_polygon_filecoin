package io.traxa.ui.main

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fondesa.kpermissions.extension.checkPermissionsStatus
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.fondesa.kpermissions.isGranted
import io.traxa.R
import io.traxa.databinding.ActivityMainBinding
import io.traxa.models.Message
import io.traxa.services.CameraService
import io.traxa.services.LocationService
import io.traxa.services.Prefs
import io.traxa.services.RecordingService
import io.traxa.ui.upload.UploadActivity
import io.traxa.utils.base.LocationActivity
import io.traxa.utils.listenOrientationChanges
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


/**
 * Main activity with fullscreen camera and record capabilities
 */
class MainActivity : LocationActivity() {

    private lateinit var startRecordingSound: MediaPlayer
    private lateinit var stopRecordingSound: MediaPlayer

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val prefs: Prefs by inject()

    /**
     * Service used to interface with the camera
     */
    private val cameraService by lazy { CameraService(this, binding.cameraPreviewView) }

    /**
     * Service used to start and stop recordings.
     * It will use [CameraService] to take pictures and videos
     */
    private val recordingService by lazy { RecordingService(this, cameraService) }


    /**
     * General permission warning message
     */
    private val permissionMessage = Message(
        "Grant permissions",
        Message.Type.ERROR
    ) { onResume() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Hide status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        //View binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        //Initialize record sounds
        startRecordingSound = MediaPlayer.create(applicationContext, R.raw.record_start).also {
            it.isLooping = false
        }

        stopRecordingSound = MediaPlayer.create(applicationContext, R.raw.record_stop).also {
            it.isLooping = false
        }

        //Listen to UI changes
        listen()
    }

    /**
     * Listen for [viewModel] livedata,
     * orientation and location updates
     */
    private fun listen() {
        listenOrientationChanges {
            viewModel.deviceOrientation.value = it

            if (it == Surface.ROTATION_90 || it == Surface.ROTATION_270)
                viewModel.screenOrientation.value = Configuration.ORIENTATION_LANDSCAPE
            else viewModel.screenOrientation.value = Configuration.ORIENTATION_PORTRAIT
        }

        //Change camera orientation
        viewModel.defaultCamera.observe(this) {
            if (cameraService.isCameraReady()) recordingService.init(
                viewModel.deviceOrientation.value!!,
                viewModel.cameraSelector(),
                viewModel.selectedCameraMode().type
            )
        }

        viewModel.recordingFiltered.observe(this) {
            if (it == true) {
                binding.scanner.start()
                viewModel.recordingEnabled.value = false
                binding.imgRecord.tag = true

                startRecordingSound.start()
                recordingService.startRecordingWithLiveLocation(
                    viewModel.currentLocation,
                    viewModel.fps,
                    viewModel.deviceOrientation.value!!,
                    this
                )

                lifecycleScope.launch {
                    delay(4000)
                    viewModel.recording.value = false
                }

            } else if (it == false && binding.imgRecord.tag == true) {
                binding.scanner.stop()
                binding.imgRecord.tag = false
                viewModel.recordingEnabled.value = true

                stopRecordingSound.start()
                recordingService.stopRecording()

                val dialog = ConfirmationDialog()
                dialog.onFinish = { uuid, color ->
                    recordingService.processRecording()
                    viewModel.addContainer(uuid, recordingService.latestRecordingId, color)

                    prefs.setLatestRecordingId(recordingService.latestRecordingId)
                    prefs.clearUploadStartTime()

                    startActivity(Intent(this, UploadActivity::class.java)
                        .putExtra("fromMain", true))

                    finish()
                }

                dialog.show(supportFragmentManager, null)
            }
        }

        //Update location livedata
        locationService.locationUpdateCallback = {
            if (it.isNotEmpty()) {
                viewModel.recordingEnabled.value = true
                viewModel.currentLocation.value = it.first()
            }
        }

        //Show messages from LocationService
        locationService.messageCallback = {
            viewModel.recordingEnabled.value = false
            viewModel.message.value = it
        }

        locationService.statusCallback = {
            when (it.type) {
                LocationService.StatusType.LOCATION_OK ->
                    if (viewModel.message.value?.text == getString(R.string.waiting_location)) {
                        binding.imgRecord.setImageResource(R.drawable.ic_record)
                        viewModel.message.value = null
                    }

                LocationService.StatusType.WAITING_LOCATION -> {
                    if (viewModel.recording.value != false)
                        viewModel.recording.value = false
                }

                LocationService.StatusType.NO_GPS -> {
                    if (viewModel.recording.value != false)
                        viewModel.recording.value = false
                }
            }
        }

        viewModel.recordingEnabled.observe(this) {
            if (!it) binding.imgRecord.setImageResource(R.drawable.ic_record_disabled)
            else if (viewModel.recording.value == false) binding.imgRecord.setImageResource(R.drawable.ic_record)
        }

        viewModel.deviceOrientation.observe(this) {
            recordingService.deviceOrientation = it
        }

//        binding.imgAbout.setOnLongClickListener {
//            startActivity(Intent(this, UploadActivity::class.java))
//            true
//        }

    }

    /**
     * Start camera with [cameraService] when surfaceView is available
     */
    private fun startCamera() {
        if (binding.cameraPreviewView.isAvailable) {
            startCameraPreview()
        } else binding.cameraPreviewView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    binding.cameraPreviewView.post { startCameraPreview() }
                    binding.cameraPreviewView.surfaceTextureListener = null
                }
            }
    }

    private fun startCameraPreview() {
        recordingService.init(
            viewModel.deviceOrientation.value!!,
            viewModel.cameraSelector(),
            viewModel.selectedCameraMode().type
        )
    }

    override fun onStop() {
        if (checkPermissions()) {
            if (viewModel.recording.value == true)
                viewModel.recording.value = false

            cameraService.stop()
        }

        locationService.stopLocationUpdates()
        super.onStop()
    }

    override fun onPause() {
        if (checkPermissions()) {
            if (viewModel.recording.value == true)
                viewModel.recording.value = false

            cameraService.stop()
        }

        locationService.stopLocationUpdates()
        super.onPause()
    }

    override fun onBackPressed() {
        if (viewModel.quickOptionsVisible.value == true)
            viewModel.quickOptionsVisible.value = false
        else super.onBackPressed()
    }

    private fun checkPermissions() = checkPermissionsStatus(
        Manifest.permission.CAMERA,
        *locationService.locationPermissions
    ).map { it.isGranted() }.reduce { a, b -> a && b }

    override fun onResume() {
        super.onResume()

        //Ask camera and location permissions
        if (checkPermissions()) {
            startCamera()
            locationService.startLocationUpdates()
        } else permissionsBuilder(
            Manifest.permission.CAMERA,
            *locationService.locationPermissions
        ).build().send {
            if (checkPermissions()) {
                startCamera()
                locationService.startLocationUpdates()
            } else viewModel.message.value = permissionMessage
        }
    }


}