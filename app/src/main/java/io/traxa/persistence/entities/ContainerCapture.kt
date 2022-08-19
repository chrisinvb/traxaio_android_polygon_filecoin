package io.traxa.persistence.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ContainerCapture(
    @PrimaryKey val uid: String,
    val recordingId: Int,
    val timestamp: Long,
    val color: ColorType,
    var storageType: String? = null,
    var storageLink: String? = null,
    var containerIds: String? = null,
    var containerPositions: String? = null,
    var containerType: String? = null,
    var mintTimestamp: Long? = null
) {

    fun isMinted(): Boolean {
        return storageType != null
                && storageLink != null
                && containerIds != null
                && containerPositions != null
                && containerType != null
                && mintTimestamp != null
    }
}