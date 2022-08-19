package io.traxa.models

import java.io.Serializable

data class PictureFormat(
    val width: Int,
    val height: Int,
    val format: Int,
    var rotation: Int
) : Serializable