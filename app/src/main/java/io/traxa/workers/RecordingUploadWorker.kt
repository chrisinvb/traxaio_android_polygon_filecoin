package io.traxa.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.traxa.modes.CaptureModeProcessor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class RecordingUploadWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val workManager: WorkManager by inject()
    private var recordingId: Int = 0

    private fun uploadFileTask(
        file: File,
        recordingId: Int,
        captureFileId: Int) = CaptureModeProcessor.uploadFileTaskWithRetry(
            file,
            recordingId,
            captureFileId
        )

    @SuppressLint("EnqueueWork")
    override suspend fun doWork(): Result {
        recordingId = inputData.getInt("recordingId", 0)
        val isAutomateImageDeleteAllow = inputData.getBoolean("isAutomateImageDeleteAllow", false)
        val filepaths = inputData.getStringArray("filepaths") ?: return Result.failure()
        val captureFileIds = inputData.getIntArray("captureFileIds") ?: return Result.failure()
        val files = filepaths.map { File(it) }

        //Enqueue upload tasks
        files.forEachIndexed { index, file ->
            workManager.beginWith(
                uploadFileTask(
                    file,
                    recordingId,
                    captureFileIds[index]
                )
            ).enqueue()
        }

        return Result.success()
    }
}