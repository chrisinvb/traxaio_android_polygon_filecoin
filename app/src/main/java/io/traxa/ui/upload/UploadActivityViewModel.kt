package io.traxa.ui.upload

import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import androidx.work.WorkManager
import dispatch.core.launchIO
import io.traxa.R
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.CaptureFile
import io.traxa.persistence.entities.ContainerCapture
import io.traxa.persistence.entities.RecordingStatus
import io.traxa.repositories.PlayerTokenRepository
import io.traxa.services.Prefs
import io.traxa.services.network.AwsService
import io.traxa.ui.containers.detail.ContainerDetailActivity
import io.traxa.utils.Constants
import io.traxa.workers.UploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class UploadActivityViewModel(app: Application) : AndroidViewModel(app), KoinComponent {

    private val prefs: Prefs by inject()
    private val db: AppDatabase by inject()
    private val awsService: AwsService by inject()


    private val tokenRepository: PlayerTokenRepository by inject()
    private val recordingDao = db.recordingDao()
    private val containerDao = db.containerDao()
    private val captureFileDao = db.captureFileDao()
    private val explorerWallet =
        "https://viewblock.io/arweave/address/OK_m2Tk41N94KZLl5WQSx_-iNWbvcp8EMfrYsel_QeQ"
    private val workManager = WorkManager.getInstance(app)
    private var levelupSound = MediaPlayer.create(app.applicationContext, R.raw.levelup).also {
        it.isLooping = false
        it.setVolume(.8f, .8f)
    }

    var waitingTime: Float = Constants.waitingTime

    suspend fun getContainerCount() = containerDao.getContainerCount()

    fun calculateSecondsPassed() = prefs.getUploadStartTime().let {
        if (it == 0L) 0
        else (System.currentTimeMillis() / 1000) - it
    }

    val secondsPassed = MutableLiveData(calculateSecondsPassed())

    val container = MutableLiveData<ContainerCapture>()

    val timeLeft = secondsPassed.map {
        val timeLeftSeconds = (waitingTime * 60) - it
        val minutes = (timeLeftSeconds / 60).toInt()
        val seconds = (timeLeftSeconds % 60).toInt()

        "$minutes:${"$seconds".padStart(2, '0')}"
    }

    private val latestRecordingId = MutableLiveData<Int>()
    private val latestRecordingCaptureFile = MutableLiveData<CaptureFile>()

    val recordingStatus = latestRecordingId
        .switchMap { recordingDao.loadAllByIdsLiveData(it) }
        .map { it.firstOrNull() }
        .map { it?.status }

    val message = latestRecordingId
        .switchMap { recordingStatus }
        .map {
            if (it == RecordingStatus.DONE) R.string.processing
            else R.string.uploading_picture
        }

    val recordingUploadProgress = latestRecordingId
        .switchMap { workManager.getWorkInfosByTagLiveData("$it") }
        .map { UploadWorker.getProgress(it).toInt() }

    val isMonitorWalletVisible = MutableLiveData(false)

    fun shareDiscord(view: View) {
        val context = view.context
        context.startActivity(Intent.parseUri(Constants.DISCORD, 0))
    }

    fun shareFeedback(view: View) {
        val context = view.context
        context.startActivity(Intent.parseUri(Constants.FEEDBACK, 0))
    }

    fun monitorWallet(view: View) {
        val context = view.context

        val uid = container.value?.uid ?: ""
        context.startActivity(
            Intent(context, ContainerDetailActivity::class.java)
                .putExtra("uid", uid)
        )
    }

    suspend fun checkToken() {
        val tokens = withContext(Dispatchers.IO) { tokenRepository.getPlayerTokens() }
        val token = tokens.find { it.captureKey == ""+latestRecordingId.value!! }
        if (token != null) {
            levelupSound.start()
            isMonitorWalletVisible.value = true
        }else {
            delay(30 * 1000L)
            checkToken()
        }
    }

    suspend fun uploadThumbnail(thumbnail: File) {
        val recordingId = prefs.getLatestRecordingId()
        val captureFile = captureFileDao.loadAllByRecordingId(recordingId)
            .lastOrNull()

        Log.d("UploadActivityViewModel", "captureFile: $captureFile")

        val filename = captureFile?.aliasFilename
            ?.replace(".mp4", ".${thumbnail.extension}")

        Log.d("UploadActivityViewModel", "uploadThumbnail: $filename")
        awsService.uploadFile(thumbnail, filename, "thumbnails")
    }

    init {
        viewModelScope.launchIO {
            val recordingId = prefs.getLatestRecordingId()

            latestRecordingId.postValue(recordingId)
            container.postValue(containerDao.getByRecordingId(recordingId).firstOrNull())
        }
    }
}