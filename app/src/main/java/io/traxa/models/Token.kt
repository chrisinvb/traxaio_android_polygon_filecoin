package io.traxa.models

data class Token (
    val captureKey: String,
    val storageType: String,
    val storageLink: String,
    val containerIds: String,
    val containerPositions: String,
    val containerTypes: String,
    val timestamp: Long
)