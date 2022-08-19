package io.traxa.utils

import androidx.room.TypeConverter
import io.traxa.models.CameraMode
import io.traxa.persistence.entities.RecordingStatus

class Converters {
    @TypeConverter
    fun fromCameraMode(mode: CameraMode.Type): Int = when (mode) {
        is CameraMode.Type.Video -> CameraMode.VIDEO
    }

    @TypeConverter
    fun toCameraMode(value: Int): CameraMode.Type = when (value) {
        CameraMode.VIDEO -> CameraMode.Type.Video()
        else -> CameraMode.Type.Video()
    }

    @TypeConverter
    fun fromRecordingStatus(status: RecordingStatus): String = status.name

    @TypeConverter
    fun toRecordingStatus(status: String): RecordingStatus = enumValueOf(status)
}