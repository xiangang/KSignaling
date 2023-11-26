package com.nxg.im.core.middleware.minio

import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.utils.SnowflakeUtils
import com.typesafe.config.ConfigFactory
import io.ktor.http.content.*
import io.ktor.server.config.*
import io.minio.*
import io.minio.errors.MinioException
import io.minio.http.Method

object KSignalingMinioClient {

    private const val TAG = "KSignalingMinioClient"

    val config = HoconApplicationConfig(ConfigFactory.load())
    val url = config.property("ktor.minio.url").getString()
    val bucket = config.property("ktor.minio.bucket").getString()
    val accessKey = config.property("ktor.minio.accessKey").getString()
    val secretKey = config.property("ktor.minio.secretKey").getString()

    // Create a minioClient with the MinIO server playground, its access key and secret key.
    private val minioClient: MinioClient by lazy {
        LOGGER.info("$TAG minioClient create ")
        val client = MinioClient.builder()
            .endpoint(url)
            .credentials(accessKey, secretKey)
            .build()

        client
    }

    private fun init() {
        try {
            // Make bucket if not exist.
            val found =
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
            if (!found) {
                // Make a new bucket called BUCKET
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())

            } else {
                LOGGER.info("$TAG Bucket $bucket already exists.")
            }
        } catch (e: MinioException) {
            LOGGER.info("$TAG Error occurred: ", e)
            LOGGER.info("$TAG HTTP trace: " + e.httpTrace())
        }
    }

    fun upload(partData: PartData.FileItem): String? {
        LOGGER.info("$TAG partData originalFileName ${partData.originalFileName}")
        LOGGER.info("$TAG partData name ${partData.name}")
        try {
            val ext = partData.originalFileName?.substringAfterLast(".")
            val fileName = "${SnowflakeUtils.snowflake.nextId()}_${partData.originalFileName}"
            LOGGER.info("$TAG partData fileName $fileName")
            partData.streamProvider().use {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(fileName)
                        .stream(
                            it, -1, ObjectWriteArgs.MIN_MULTIPART_SIZE.toLong()
                        ) // 设置分片大小，这里设置为10MB
                        .contentType("application/octet-stream").build()
                )
                LOGGER.info(
                    "$TAG {} is successfully uploaded as object {} to bucket {}",
                    fileName,
                    fileName,
                    bucket,
                )
                // 获取文件的URL
                val url: String = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .`object`(fileName)
                        .method(Method.GET)
                        .expiry(7 * 24 * 3600) // 设置URL的过期时间（以秒为单位）
                        .build()
                )

                LOGGER.info(
                    "$TAG {} url is {}",
                    fileName,
                    url,
                )
                return url
            }
        } catch (e: Exception) {
            LOGGER.info("$TAG Error occurred", e)
            //LOGGER.info("$TAG HTTP trace: " + e.httpTrace())
        }
        return null
    }
}