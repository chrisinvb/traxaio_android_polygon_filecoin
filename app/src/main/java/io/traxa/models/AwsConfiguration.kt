package io.traxa.models

data class AwsConfiguration(
    val region: String,
    val bucketName: String,
    val accessKey: String,
    val secretKey: String,
    val folder: String? = "notprocessed"
)