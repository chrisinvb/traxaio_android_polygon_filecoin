package io.traxa.ui.main

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.lifecycle.*
import androidx.work.WorkManager
import io.traxa.models.CameraMode
import io.traxa.models.Message
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.ColorType
import io.traxa.persistence.entities.ContainerCapture
import io.traxa.ui.containers.list.ContainerListActivity
import io.traxa.ui.settings.SettingsActivity
import io.traxa.utils.MeanColorClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class MainViewModel(val app: Application) : AndroidViewModel(app), KoinComponent {

    private val workManager = WorkManager.getInstance(app)
    private val database: AppDatabase by inject()
    private val recordingDao = database.recordingDao()
    private val containerDao = database.containerDao()

    val screenOrientation = MutableLiveData(Configuration.ORIENTATION_PORTRAIT)
    val deviceOrientation = MutableLiveData(Surface.ROTATION_0)
    val viewRotation = Transformations.map(deviceOrientation) {
        when (it) {
            Surface.ROTATION_0 -> 0F
            Surface.ROTATION_90 -> -90F
            Surface.ROTATION_270 -> 90F
            Surface.ROTATION_180 -> 180F
            else -> 0F
        }
    }

    val fps = 30.0
    val quickOptionsVisible = MutableLiveData(false)
    val message = MutableLiveData<Message?>()
    val defaultCamera = MutableLiveData(LENS_FACING_BACK)

    val recording = MutableLiveData(false)
    val currentLocation = MutableLiveData<Location>()
    val recordingEnabled = MutableLiveData(true)

    var lastPressed = 0L
    private val spamPreventPeriod = 500

    val recordingFiltered = Transformations.map(recording) {
        val time = System.currentTimeMillis()
        if (time - lastPressed > spamPreventPeriod) {
            lastPressed = time
            return@map it
        }

        val nullBoolean: Boolean? = null
        nullBoolean
    }

    fun addContainer(uuid: String, recordingId: Int, color: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val colorType = if (color == Color.RED)
                ColorType.RED else MeanColorClassifier.classify(color)

            containerDao.insertContainer(
                ContainerCapture(
                    uuid,
                    recordingId,
                    System.currentTimeMillis(),
                    colorType
                )
            )
        }
    }

    fun selectedCameraMode() = CameraMode("Video", CameraMode.Type.Video())

    fun toggleRecording(view: View) {
        if (recordingEnabled.value!!) recording.value = !recording.value!!
        else Toast.makeText(view.context, message.value?.text, Toast.LENGTH_SHORT).show()
    }

    fun openContainerYard(view: View) {
        view.context.startActivity(Intent(view.context, ContainerListActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun messageClicked(view: View) {
        message.value?.action?.invoke()
        if (message.value?.action != null) message.value = null
    }

    fun openSettings(view: View) {
        view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
    }

    //Return camera selector from selected lens
    fun cameraSelector(): CameraSelector {
        val isDefaultCameraBack = defaultCamera.value == LENS_FACING_BACK

        return if (isDefaultCameraBack) CameraSelector.DEFAULT_BACK_CAMERA
        else CameraSelector.DEFAULT_FRONT_CAMERA
    }
}