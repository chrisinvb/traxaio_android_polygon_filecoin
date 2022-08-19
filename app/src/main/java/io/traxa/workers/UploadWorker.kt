package io.traxa.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.CaptureFile
import io.traxa.persistence.entities.Recording
import io.traxa.persistence.entities.RecordingStatus
import io.traxa.persistence.entities.UploadStatus
import io.traxa.services.network.AwsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Worker used to upload a single file to AWS.
 * "filepath" is the input string with the path of the file to upload
 */
class UploadWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent, TransferListener {

    private val workManager = WorkManager.getInstance(context)
    private val awsService: AwsService by inject()
    private val database: AppDatabase by inject()
    private val mutex = Mutex(true)
    private var uploadId: Int = -1
    private var uploadFile: File? = null

    private lateinit var recording: Recording
    private lateinit var captureFile: CaptureFile

    @ExperimentalCoroutinesApi
    override suspend fun doWork(): Result {
        var lockFile: File? = null

        try {
            val filepath = inputData.getString("filepath") ?: return Result.failure()
            val expedited = inputData.getBoolean("expedited", true)
            val recordingId = tags.first { it.matches(Regex("^[0-9]+")) }.toInt()
            val captureFileId = tags.first { it.matches(Regex("^cfid_[0-9]+")) }
                .substring(5)
                .toInt()

            recording = database.recordingDao().loadAllByIds(recordingId).first()
            captureFile = database.captureFileDao().loadAllByIds(captureFileId).first()
            uploadFile = File(filepath)

            //Lock file used to determine if upload was not completed and the work retried
            lockFile = File(uploadFile!!.parent, uploadFile!!.nameWithoutExtension + ".lock")

            val observer = if (!lockFile.exists()) awsService.uploadFile(
                uploadFile!!,
                captureFile.aliasFilename
            ).also {
                lockFile.createNewFile()
                lockFile.writeText("${it.id}")
            } else awsService.resumeUpload(lockFile.readText().toInt())

            captureFile.status = UploadStatus.PENDING
            database.captureFileDao().update(captureFile)

            recording.status = RecordingStatus.UPLOADING
            database.recordingDao().update(recording)

            observer.setTransferListener(this)
            uploadId = observer.id


            //Wait here until the upload is completed (or error)
            mutex.lock()

            return if (observer.state == TransferState.COMPLETED) {
                val query = WorkQuery.Builder.fromTags(
                    listOf("${recording.uid}", UploadWorker::class.java.name)
                ).build()

                //Notify upload is completed
                setProgressAsync(
                    workDataOf("uploadCompleted" to true)
                )

                //Set recording to "complete" when every file of this recording is successfully uploaded
                var completed = true

                //Critical part, required to run in mutual exclusion
                sharedMutex.acquire()

                //Wait for previous worker to return a successful result
                delay(400)

                val list = workManager.getWorkInfos(query).await()

                for (workInfo in list) {
                    if (workInfo.id != id) {
                        val progress = getProgress(workInfo)
                        if (progress < 100 || !workInfo.state.isFinished) {
                            completed = false
                            break
                        }
                    }
                }

                val currentRecordingStatus = database.recordingDao()
                    .loadAllByIds(recordingId)
                    .first()
                    .status

                if (completed && currentRecordingStatus != RecordingStatus.UPLOAD_ERROR) {
                    recording.status = RecordingStatus.DONE
                    database.recordingDao().update(recording)
                }

                sharedMutex.release()


                lockFile.delete()
                Result.success(workDataOf("filepath" to filepath))
            } else if (runAttemptCount > 2) {
                markUploadAsError()
                Result.failure()
            } else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            return if (runAttemptCount > 2) {
                markUploadAsError()

                //Work is failed, cancel upload and lock file
                if (uploadId != -1) {
                    awsService.cancelUpload(uploadId)
                    uploadFile?.delete()
                    if (lockFile?.exists() == true) lockFile.delete()
                }

                Result.failure()
            } else Result.retry()
        }
    }

    private fun markUploadAsError() {
        recording.status = RecordingStatus.UPLOAD_ERROR
        database.recordingDao().update(recording)

        captureFile.status = UploadStatus.UPLOAD_ERROR
        database.captureFileDao().update(captureFile)
    }

    override fun onStateChanged(id: Int, state: TransferState) {
        if (state == TransferState.IN_PROGRESS) {
            CoroutineScope(coroutineContext).launch {
                recording.status = RecordingStatus.UPLOADING
                database.recordingDao().update(recording)

                captureFile.status = UploadStatus.UPLOADING
                database.captureFileDao().update(captureFile)
            }

        }

        if (state == TransferState.PAUSED) {
            CoroutineScope(coroutineContext).launch {
                captureFile.status = UploadStatus.PAUSED
                database.captureFileDao().update(captureFile)
            }

        }

        if (state == TransferState.COMPLETED) {
            CoroutineScope(coroutineContext).launch {
                captureFile.status = UploadStatus.DONE
                database.captureFileDao().update(captureFile)
            }

            mutex.unlock()
        }
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        setProgressAsync(
            workDataOf(
                "bytesCurrent" to bytesCurrent,
                "bytesTotal" to bytesTotal
            )
        )

        if (captureFile.status != UploadStatus.UPLOADING) CoroutineScope(coroutineContext).launch {
            captureFile.status = UploadStatus.UPLOADING
            database.captureFileDao().update(captureFile)
        }
    }

    override fun onError(id: Int, ex: Exception) {
        mutex.unlock()
    }

    companion object {

        private val sharedMutex = Semaphore(1)

        private fun getProgress(workInfo: WorkInfo): Int {
            val current = workInfo.progress.getLong("bytesCurrent", 0)
            val total = workInfo.progress.getLong("bytesTotal", 0)
            val uploadCompleted = workInfo.progress.getBoolean("uploadCompleted", false)

            if (uploadCompleted) return 100
            return when {
                total != 0L -> (current / total).toInt() * 100
                workInfo.state == WorkInfo.State.SUCCEEDED -> 100
                else -> 0
            }
        }

        fun getProgressLiveData(
            workInfo: LiveData<MutableList<WorkInfo>>
        ) = workInfo.map {
            var percentTotal = 0
            for (file in it) {
                percentTotal += getProgress(file)
            }

            if (it.isEmpty()) 100
            else percentTotal / it.size
        }

        fun getProgress(
            workInfo: MutableList<WorkInfo>
        ) : Double {
            var percentTotal = 0.0
            for (file in workInfo) {
                percentTotal += getProgress(file)
            }

            return if (workInfo.isEmpty()) 100.0
            else percentTotal / workInfo.size
        }
    }

}