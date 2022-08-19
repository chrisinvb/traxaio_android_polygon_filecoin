package io.traxa.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dispatch.core.dispatcherProvider
import io.traxa.services.FileProcessingService
import io.traxa.services.Prefs
import io.traxa.utils.dateString
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*

open class CaptureVideoModeWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val fileProcessingService: FileProcessingService by inject()
    private val prefs: Prefs by inject()
    private val separator = "_"

    override suspend fun doWork(): Result {

        val recordingId = inputData.getInt("recordingId", 0)

        val tempFile = File(context.getExternalFilesDir("captures"), "video.mp4")
        val srtFile = File(context.getExternalFilesDir("captures"), "gps_subtitles.srt")
        val mergedFile = File(context.getExternalFilesDir("captures"), "video_merge.mp4")

        return try {
            val videoFiles = withContext(kotlin.coroutines.coroutineContext.dispatcherProvider.io) {

                if (mergedFile.exists()) mergedFile.delete()

                //Merge subtitle with video.mp4
                fileProcessingService.mergeSrtWithVideo(srtFile, tempFile, mergedFile)

                //Copy metadata from original video file
                Runtime.getRuntime()
                    .exec("touch -r ${tempFile.absolutePath} ${mergedFile.absolutePath}")

                //Remove unused files
                if (srtFile.exists()) srtFile.delete()
                if (tempFile.exists()) tempFile.delete()

                val dateTime = Date().time
                //val uuid = UUID.randomUUID().toString()

                val accountId = prefs.getPlayerId()
                val file = File(context.getExternalFilesDir("captures"),
                    "$recordingId$separator${dateString(dateTime)}_s00_$accountId.mp4")

                mergedFile.renameTo(file)

                //Return file array
                listOf(file)
            }

            Result.success(
                workDataOf(
                    "recordingId" to recordingId,
                    "filepaths" to videoFiles.map { it.absolutePath }.toTypedArray(),
                )
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }


}