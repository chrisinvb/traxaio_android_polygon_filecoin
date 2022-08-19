package io.traxa.persistence.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.traxa.R

enum class UploadStatus(val stringResource: Int) {
    PENDING(R.string.pending),
    UPLOADING(R.string.uploading),
    PAUSED(R.string.paused),
    DONE(R.string.done),
    UPLOAD_ERROR(R.string.upload_error)
}

@Entity
data class CaptureFile(
    val filename: String,
    val recordingId: Int,
    val fileSize: Double,
    var aliasFilename: String = filename,
    var status: UploadStatus = UploadStatus.PENDING,
    @PrimaryKey(autoGenerate = true) var uid: Int = 0
)