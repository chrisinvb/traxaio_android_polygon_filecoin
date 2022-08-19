package io.traxa.services.network

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import io.traxa.models.AwsConfiguration
import io.traxa.models.TestResult
import java.io.File

/**
 * Service used to connect and interface with AWS
 */
class AwsService(private val context: Context) {

    private lateinit var s3Client: AmazonS3Client

    private var transferUtility: TransferUtility? = null
    private var configuration: AwsConfiguration? = null

    fun isReady() = transferUtility != null

    /**
     * Close connection with internal database.
     * Using reflection because closeDB method is private.
     *
     * Useful for tests
     */
    fun stop() {
        if (this::s3Client.isInitialized) {
            s3Client.shutdown()
            val dbUtilClass =
                Class.forName("com.amazonaws.mobileconnectors.s3.transferutility.TransferDBUtil")

            val dbUtilField = TransferUtility::class.java.getDeclaredField("dbUtil")
            dbUtilField.isAccessible = true

            val dbUtil = dbUtilClass.cast(dbUtilField.get(transferUtility))
            val method = dbUtilClass.getDeclaredMethod("closeDB")
            method.isAccessible = true
            method.invoke(dbUtil)
        }
    }

    /**
     * Setup connection with given configuration
     *
     * @param configuration AWS configuration
     */
    fun setup(configuration: AwsConfiguration) {
        this.configuration = configuration

        TransferNetworkLossHandler.getInstance(context)

        s3Client = AmazonS3Client(
            BasicAWSCredentials(
                configuration.accessKey,
                configuration.secretKey
            ), Region.getRegion(configuration.region)
        )

        //Max 2 concurrent upload
        val tuOptions = TransferUtilityOptions()
        tuOptions.transferThreadPoolSize = 2

        transferUtility = TransferUtility.builder()
            .s3Client(s3Client)
            .defaultBucket(configuration.bucketName)
            .transferUtilityOptions(tuOptions)
            .context(context)
            .build()

    }

    /**
     * Start uploading a file
     *
     * @param file File to upload
     * @param filename Alternative filename, default null (same as the file)
     */
    fun uploadFile(file: File, filename: String? = null, folder: String? = null): TransferObserver {
        checkDependencies()

        if (transferUtility == null || configuration == null)
            throw Exception("Please setup AwsService with setup()")

        return if(folder != null) transferUtility!!.upload(
            "$folder/" + (filename ?: file.name),
            file
        ) else transferUtility!!.upload(
            (configuration?.folder?.trim('/') ?: "notprocessed") + "/" + (filename ?: file.name),
            file
        )
    }

    fun pauseUpload(id: Int) {
        checkDependencies()
        transferUtility!!.pause(id)
    }

    fun resumeUpload(id: Int): TransferObserver {
        checkDependencies()
        Log.e("RecordingService", id.toString())
        return transferUtility!!.resume(id)
    }

    fun cancelUpload(id: Int) {
        checkDependencies()
        transferUtility!!.cancel(id)
    }


    private fun checkDependencies() {
        if (transferUtility == null || configuration == null)
            throw Exception("Please setup AwsService with setup()")
    }

    companion object {

        /**
         * Test if the client can connect with the given AWS configuration
         *
         * @param configuration [AwsConfiguration] to test
         */
        fun testConnection(configuration: AwsConfiguration): TestResult {
            try {
                val client = AmazonS3Client(
                    BasicAWSCredentials(
                        configuration.accessKey,
                        configuration.secretKey
                    ), Region.getRegion(configuration.region)
                )

                for (bucket in client.listBuckets()) {
                    if (bucket.name == configuration.bucketName) {
                        var bucketLocation = client.getBucketLocation(bucket.name).lowercase()
                        if (bucketLocation == "us") bucketLocation = "us-east-1"

                        if (configuration.region.contains(bucketLocation))
                            return TestResult(true, "Test passed successfully")
                    }
                }

                return TestResult(false, "Cannot find bucket in ${configuration.region}")
            } catch (e: Exception) {
                return TestResult(false, "Can't establish a connection")
            }
        }
    }

}