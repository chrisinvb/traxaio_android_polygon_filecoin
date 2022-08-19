package io.traxa.modes.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dispatch.core.dispatcherProvider
import io.traxa.modes.CaptureMode
import io.traxa.modes.CaptureModeProcessor
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.RecordingStatus
import io.traxa.utils.video.srt.SRT
import io.traxa.utils.video.srt.SRTInfo
import io.traxa.utils.video.srt.SRTWriter
import io.traxa.workers.CaptureFileAdderWorker
import io.traxa.workers.CaptureVideoModeWorker
import io.traxa.workers.RecordingUploadWorker
import kotlinx.coroutines.*
import org.koin.core.component.inject
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext


open class CaptureVideoModeProcessor(
    val context: Context
) : CaptureModeProcessor() {

    inner class PollingTask(
        val running: ((Int, Long) -> Unit),
        val cancel: ((Int, Long) -> Unit)? = null
    )

    private val database: AppDatabase by inject()
    private val recordingDao = database.recordingDao()

    /**
     * WorkManager used to enqueue upload and fileRemove tasks
     */
    private val workManager: WorkManager by inject()

    private var locationVideoTrack: SRTInfo? = null
    private var locationSubtitleJob: Job? = null


    private val locationSubtitleTask: ((Int, Long) -> Unit) = { i, deltaT ->
        val start = deltaT * i
        val end = start + deltaT
        val lat = location?.value?.latitude
        val lon = location?.value?.longitude


        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (lat != null && lon != null)
            locationVideoTrack!!.add(
                SRT(
                    i,
                    Date(calendar.timeInMillis + start),
                    Date(calendar.timeInMillis + end),
                    "$lat $lon"
                )
            )
    }

    private val pollingTasks = ArrayList<PollingTask>().also {
        it.add(PollingTask(locationSubtitleTask))
    }


    override fun start(captureMode: CaptureMode, recordingId: Int) {
        super.start(captureMode, recordingId)

        locationVideoTrack = null
        locationVideoTrack = SRTInfo()

        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        locationSubtitleJob = CoroutineScope(dispatcher).launch {
            var i = 0
            val deltaT = 200L

            try {
                while (isActive) {
                    pollingTasks.onEach { it.running(i, deltaT) }

                    i++
                    delay(deltaT)
                }
            } catch (e: Exception) {
                pollingTasks.onEach { it.cancel?.invoke(i, deltaT) }
            }
        }
    }

    protected open fun getProcessorWorkRequest() =
        OneTimeWorkRequestBuilder<CaptureVideoModeWorker>()
            .addTag("processor_$recordingId")
            .setInputData(workDataOf("recordingId" to recordingId))
            .build()

    private fun getRecordingUploadWorker() = OneTimeWorkRequestBuilder<RecordingUploadWorker>()
        .addTag("processor_$recordingId")
        .setInputData(workDataOf("isAutomateImageDeleteAllow" to true))
        .build()

    private fun getCaptureFileAdderWorker() = OneTimeWorkRequestBuilder<CaptureFileAdderWorker>()
        .addTag("processor_$recordingId")
        .build()

    @SuppressLint("EnqueueWork")
    override suspend fun stop(captureMode: CaptureMode) {
        locationSubtitleJob!!.cancel("Recording stopped")
        //super.stop(captureMode)
    }

    override suspend fun process() {
        val srtFile = File(context.getExternalFilesDir("captures"), "gps_subtitles.srt")

        if (srtFile.exists()) srtFile.delete()
        val recording = recordingDao.loadAllByIds(recordingId).first()

        withContext(coroutineContext.dispatcherProvider.io) {
            recording.status = RecordingStatus.PENDING
            recordingDao.update(recording)
        }

        //Generate .srt file
        withContext(coroutineContext.dispatcherProvider.io) {
            SRTWriter.write(srtFile, locationVideoTrack!!)
        }

        workManager.beginWith(getProcessorWorkRequest())
            .then(getCaptureFileAdderWorker())
            .then(getRecordingUploadWorker())
            .enqueue()
    }

}