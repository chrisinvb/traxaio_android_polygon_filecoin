package io.traxa.persistence.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.traxa.BuildConfig
import io.traxa.R
import io.traxa.models.CameraMode

enum class RecordingStatus(val stringResource: Int) {
    PENDING(R.string.pending),
    UPLOADING(R.string.uploading),
    PAUSED(R.string.paused),
    DONE(R.string.done),
    SPLITTING_VIDEO(R.string.splitting_video),
    UPLOAD_ERROR(R.string.upload_error)
}

enum class RecordingSource(val stringResource: Int) {
    APP(R.string.app_source),
    EXTERNAL_FILE(R.string.external_source)
}

@Entity
data class Recording(
    val type: CameraMode.Type,
    var status: RecordingStatus = RecordingStatus.PENDING,
    val source: RecordingSource = RecordingSource.APP,
    val appVersion: String = BuildConfig.VERSION_NAME,
    @PrimaryKey(autoGenerate = true) var uid: Int = 0
)