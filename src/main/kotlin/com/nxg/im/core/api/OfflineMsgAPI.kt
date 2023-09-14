package com.nxg.im.core.api

import com.nxg.im.core.IMCoreMessage
import com.nxg.im.core.data.bean.OfflineMsg
import com.nxg.im.core.data.redis.KSignalingRedisClient
import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.plugins.respondUnauthorized
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.Limit
import io.lettuce.core.Range
import java.nio.charset.StandardCharsets

fun Route.offlineMsgAPI() {
    //获取离线消息
    get("$API_V1/offlineMsg") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@get
        }
        val pageIndex = call.parameters["pageIndex"]?.toInt() ?: 0
        val limit = call.parameters["pageSize"]?.toInt() ?: 20
        val fromId = call.parameters["fromId"]
        val messageId = call.parameters["messageId"]
        if (fromId.isNullOrEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to HttpStatusCode.BadRequest.value,
                    "message" to "Missing fromId parameter",
                    "data" to null
                )
            )
            return@get
        }
        if (messageId.isNullOrEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to HttpStatusCode.BadRequest.value,
                    "message" to "Missing messageId parameter",
                    "data" to null
                )
            )
            return@get
        }
        // 从Redis获取离线消息
        try {
            val offset = pageIndex * limit
            val min = java.lang.Double.longBitsToDouble(messageId.toLong() + 1)
            val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
            val key = "offline:${user.uuid}-$fromId"
            LOGGER.info("offlineMessages: key $key, min $min, offset $offset, limit $limit")
            val offlineMessages = redisCommands.zrangebyscore(
                key,
                Range.create(min, Long.MAX_VALUE),
                Limit.create(offset.toLong(), limit.toLong())
            )
            val count = redisCommands.zcount(key, Range.create(min, Long.MAX_VALUE).gt(min))
            LOGGER.info("offlineMessages: count $count")
            val messages = mutableListOf<String>()
            offlineMessages.forEach {
                val imCoreMessage = IMCoreMessage.parseFrom(it.toByteArray(StandardCharsets.ISO_8859_1))
                val imMessageJson = imCoreMessage.bodyData.toStringUtf8()
                LOGGER.info("offlineMessages: imMessageJson $imMessageJson")
                messages.add(it)
            }
            LOGGER.info("offlineMessages: messages $messages")
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.OK.value,
                    "message" to "Success",
                    "data" to OfflineMsg(count, messages)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf(
                    "code" to HttpStatusCode.BadGateway.value,
                    "message" to "{${e.message}}",
                    "data" to null
                )
            )
        }
    }
}