package io.traxa.modes

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.work.*
import io.traxa.workers.UploadWorker
import org.koin.core.component.KoinComponent
import java.io.File
import java.util.concurrent.TimeUnit

abstract class CaptureModeProcessor : KoinComponent {

    var location: LiveData<Location>? = null

    protected var recordingId = -1
    protected val separator = "_"

    open fun start(captureMode: CaptureMode, recordingId: Int) {
        this.recordingId = recordingId
    }

    open suspend fun stop(captureMode: CaptureMode) {
        this.recordingId = -1
    }

    abstract suspend fun process()

    protected fun uploadFileTask(
        file: File,
        recordingId: Int,
        captureFileId: Int,
        expedited: Boolean = true
    ) = uploadFileTaskWithRetry(
        file,
        recordingId,
        captureFileId,
        expedited
    )

    companion object {

        /**
         * Work constraints (internet access)
         */
        private val uploadConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Upload image/file with automatic retry
         * @param file Image/video to be uploaded
         * @param recordingId Id of the recording (in database)
         * @return Upload work task
         */
        @SuppressLint("UnsafeOptInUsageError")
        fun uploadFileTaskWithRetry(
            file: File,
            recordingId: Int,
            captureFileId: Int,
            expedited: Boolean = true
        ) = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("$recordingId") //This will be the recording ID
            .addTag(file.name)
            .addTag("cfid_$captureFileId")
            //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            //.setConstraints(uploadConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
            .setInputData(workDataOf("filepath" to file.path, "expedited" to expedited))
            .build()

        /**
         * Upload image/file
         * @param file Image/video to be uploaded
         * @param recordingId Id of the recording (in database)
         * @return Upload work task
         */
        @SuppressLint("UnsafeOptInUsageError")
        fun uploadFileTaskWithoutRetry(
            file: File,
            recordingId: Int,
            captureFileId: Int,
            expedited: Boolean = true
        ) = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("$recordingId") //This will be the recording ID
            .addTag(file.name)
            .addTag("cfid_$captureFileId")
            //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(uploadConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
            .setInputData(workDataOf("filepath" to file.path, "expedited" to expedited))
            .build()

    }


}