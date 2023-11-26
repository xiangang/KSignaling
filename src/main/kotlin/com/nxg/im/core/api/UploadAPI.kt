package com.nxg.im.core.api

import com.nxg.im.core.middleware.minio.KSignalingMinioClient
import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.plugins.respondUnauthorized
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.uploadAPI() {
    //上传接口
    post("$API_V1/upload") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@post
        }
        LOGGER.info("uploadAPI -----------------------> ")
        val contentLength = call.request.header(HttpHeaders.ContentLength)
        LOGGER.info("uploadAPI contentLength $contentLength ")
        val multiPartData = call.receiveMultipart()
        LOGGER.info("uploadAPI multiPartData $multiPartData ")
        var uploadFileUrl: String? = null
        multiPartData.forEachPart { part ->
            LOGGER.info("uploadAPI part {}", part)
            when (part) {
                is PartData.FileItem -> {
                    try {
                        LOGGER.info("uploadAPI PartData.FileItem ${part.originalFileName}")
                        uploadFileUrl = KSignalingMinioClient.upload(part)
                        return@forEachPart
                    } catch (e: ContentTransformationException) {
                        call.respond(
                            HttpStatusCode.BadRequest, mapOf(
                                "code" to HttpStatusCode.BadRequest.value,
                                "message" to "Invalid form data.",
                                "data" to null
                            )
                        )
                        return@forEachPart
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, mapOf(
                                "code" to HttpStatusCode.InternalServerError.value,
                                "message" to "Failed to upload file: ${e.message}",
                                "data" to null
                            )
                        )
                        return@forEachPart
                    }
                }

                is PartData.FormItem -> {
                    LOGGER.info("uploadAPI PartData.FormItem ${part.value}")
                }

                else -> {
                    LOGGER.info("uploadAPI upload not supported this part data  ${it::class.java.name}}")
                }
            }
        }
        uploadFileUrl?.let {
            LOGGER.info("uploadAPI success uploadFileUrl $it")
            val shortUploadFileUrl = it.substringBefore("?", it)
            LOGGER.info("uploadAPI success shortUploadFileUrl $shortUploadFileUrl")
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.OK.value,
                    "message" to HttpStatusCode.OK.description,
                    "data" to shortUploadFileUrl
                )
            )
            return@post
        }
        LOGGER.info("uploadAPI InternalServerError ${HttpStatusCode.InternalServerError.description}")
        call.respond(
            HttpStatusCode.InternalServerError, mapOf(
                "code" to HttpStatusCode.InternalServerError.value,
                "message" to HttpStatusCode.InternalServerError.description,
            )
        )
    }
}