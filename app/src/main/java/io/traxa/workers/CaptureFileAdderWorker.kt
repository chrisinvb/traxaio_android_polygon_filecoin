package io.traxa.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.CaptureFile
import io.traxa.services.Prefs
import io.traxa.utils.dateString
import io.traxa.utils.size
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.*

class CaptureFileAdderWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val database: AppDatabase by inject()
    private val prefs: Prefs by inject()
    private val fileCaptureDao = database.captureFileDao()

    private var recordingId: Int = 0
    private val separator = "_"

    override suspend fun doWork(): Result {
        recordingId = inputData.getInt("recordingId", 0)
        val filepaths = inputData.getStringArray("filepaths") ?: return Result.failure()
        val files = filepaths.map { File(it) }
        val fileNameFormatRegex = Regex("[0-9]{1,2}_[2-9][0-9]{7}T[0-9]{9}Z(?:_s[0-9]{2})_[a-z0-9-]+")
        val captureFileIds = arrayListOf<Int>()

        val dateString = dateString(Date().time)
        files.forEach { file ->

            //Check filename format
            if (fileNameFormatRegex.containsMatchIn(file.nameWithoutExtension)) {
                captureFileIds.add(addFile(file))
            } else if (files.size == 1) {

                //Generate a new filename
                val accountId = prefs.getPlayerId()
                val alias = "$recordingId$separator$dateString${separator}_s00_$accountId." + file.extension
                captureFileIds.add(addFile(file, alias))
            }
        }

        return Result.success(
            workDataOf(
                "filepaths" to filepaths,
                "captureFileIds" to captureFileIds.toTypedArray(),
                "recordingId" to recordingId
            )
        )
    }

    private suspend fun addFile(file: File, alias: String = file.name): Int {
        return fileCaptureDao.insertAll(
            CaptureFile(
                file.name,
                recordingId,
                file.size,
                alias
            )
        ).first().toInt()
    }

}