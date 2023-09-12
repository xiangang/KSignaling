package com.nxg.im.core.plugins

import com.google.protobuf.ByteString
import com.nxg.im.core.IMCoreMessage
import com.nxg.im.core.data.RabbitMQClient
import com.nxg.im.core.data.RabbitMQCoroutineScope
import com.nxg.im.core.data.bean.IMMessage
import com.nxg.im.core.data.bean.parseIMMessage
import com.nxg.im.core.data.redis.KSignalingRedisClient
import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.repository.MessageRepository
import com.nxg.im.core.session.IMSession
import com.nxg.im.core.session.IMSessionManager
import com.nxg.im.core.signaling.Signaling
import com.nxg.im.core.signaling.SignalingManager
import com.nxg.im.core.signaling.SignalingSession
import com.nxg.im.core.sip.CallSession
import com.nxg.im.core.sip.callSessionId
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import java.nio.charset.StandardCharsets
import java.time.Duration

fun Application.configureSockets() {
    RabbitMQClient.channel.basicConsume(RabbitMQClient.QUEUE_CHAT, true, object : Consumer {
        override fun handleConsumeOk(consumerTag: String?) {
            LOGGER.info("RabbitMQClient handleConsumeOk $consumerTag")
        }

        override fun handleCancelOk(consumerTag: String?) {
            LOGGER.info("RabbitMQClient handleCancelOk $consumerTag")
        }

        override fun handleCancel(consumerTag: String?) {
            LOGGER.info("RabbitMQClient handleCancel $consumerTag")
        }

        override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
            LOGGER.info("RabbitMQClient handleShutdownSignal $consumerTag")
        }

        override fun handleRecoverOk(consumerTag: String?) {
            LOGGER.info("RabbitMQClient handleRecoverOk $consumerTag")
        }

        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray?
        ) {
            LOGGER.info("RabbitMQClient handleDelivery $consumerTag")
            body?.let {
                RabbitMQCoroutineScope.launch {
                    handleReceivedChatBytes(it)
                }
            }

        }
    })
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(ProtoBuf)
    }
    routing {
        //聊天管理
        webSocket("/chat") {
            val token = call.request.headers["Authorization"]
            if (token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is null"))
                return@webSocket
            }
            val user = JwtConfig.getUserByToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            LOGGER.info("WebSocket connection add chat user $user!")
            val imSession = IMSession(user, this)
            IMSessionManager.sessions[user.uuid] = imSession
            try {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Close -> {
                            LOGGER.info("WebSocket Removing $imSession!")
                            IMSessionManager.sessions.remove(user.uuid)
                            LOGGER.info("WebSocket connection opened")
                        }

                        is Frame.Ping -> {
                            LOGGER.info("WebSocket connection Ping $imSession")
                            // 在这里处理客户端掉线的情况
                        }

                        is Frame.Pong -> {
                            LOGGER.info("WebSocket connection Pong $imSession")
                            // 在这里处理客户端掉线的情况
                        }

                        is Frame.Binary -> {
                            val receivedBytes = frame.readBytes()
                            RabbitMQClient.channel.basicPublish(
                                RabbitMQClient.EXCHANGE_CHAT,
                                RabbitMQClient.ROUTE_KEY_CHAT,
                                AMQP.BasicProperties.Builder().build(),
                                receivedBytes
                            )
                        }

                        is Frame.Text -> {
                            val receivedText = frame.readText()
                            LOGGER.info("WebSocket chat receivedText $receivedText")
                        }

                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                LOGGER.info("WebSocket exception ${e.message}")
            } finally {
                // 连接关闭时，从映射中删除 imSession 对象
                LOGGER.info("WebSocket remove chat user $user!")
                IMSessionManager.sessions.remove(user.uuid)
            }

        }
        //房间管理
        webSocket("/room/{roomId}") { // websocketSession
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
        //信令管理
        webSocket("/signaling") {
            val token = call.request.headers["Authorization"]
            if (token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is null"))
                return@webSocket
            }
            val user = JwtConfig.getUserByToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            LOGGER.info("Adding push user $user!")
            // 将 SignalingSession 对象与用户 ID 相关联
            val newSignalingSession = SignalingSession(user, this)
            SignalingManager.sessions[user.uuid] = newSignalingSession
            //val json = Json.encodeToString(SimpleUser.serializer(), user.toSimpleUser())
            //send(json)
            // 处理从客户端发送的消息
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                    try {
                        val signaling = Json.decodeFromString(Signaling.serializer(), text)
                        LOGGER.info("signaling")
                        for (signalingUser in signaling.toUsers) {
                            LOGGER.info("signaling ${user.username} send $text to ${signalingUser.username} ")
                            SignalingManager.sendMsg2User(signalingUser.uuid, text)
                        }
                    } catch (e: Exception) {
                        LOGGER.info("signaling error ${e.message}")
                    }
                }
            }

            // 连接关闭时，从映射中删除 SignalingSession 对象
            SignalingManager.sessions.remove(user.uuid)
        }

        val sipSessions = mutableMapOf<String, CallSession>()
        webSocket("/sip") {
            val callId = callSessionId()
            val callSession = CallSession(callId, this)
            sipSessions[callId] = callSession
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        callSession.handleMessage(message)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Client disconnected
            } finally {
                sipSessions.remove(callId)
                callSession.close()
            }
        }

    }

}

private suspend fun handleReceivedChatBytes(receivedBytes: ByteArray) {
    LOGGER.info("handleReceivedChatBytes receivedBytes $receivedBytes")
    val imCoreMessage = IMCoreMessage.parseFrom(receivedBytes)
    LOGGER.info("handleReceivedChatBytes imCoreMessage $imCoreMessage")
    try {
        //bodyData存的是json
        val imMessageJson = imCoreMessage.bodyData.toStringUtf8()
        LOGGER.info("handleReceivedChatBytes imMessageJson $imMessageJson")
        val imMessage: IMMessage = imMessageJson.parseIMMessage()
        LOGGER.info("handleReceivedChatBytes ${imMessage.fromId} send $imMessageJson to ${imMessage.toId}")
        //消息去重，避免发送方重复发送
        if (!MessageRepository.isIMMessageExist(imCoreMessage.seqId)) {
            val body = imCoreMessage.bodyData.toByteArray()
            //保存聊天记录，一旦落库，就认为消息接收成功，此时可以发送ACK给发送方客户端
            val id = MessageRepository.save(imCoreMessage.seqId, imMessage)
            //如果保存成功
            if (id > 0L) {
                //发送acknowledge(protobuf)给fromId
                val imCoreMessageACK = IMCoreMessage.newBuilder().apply {
                    version = imCoreMessage.version
                    cmd = imCoreMessage.cmd
                    subCmd = imCoreMessage.subCmd
                    type = 1 // 1是acknowledge
                    logId = imCoreMessage.logId
                    seqId = imCoreMessage.seqId
                    bodyLen = body.size
                    bodyData = ByteString.copyFrom(body)
                }.build()
                //如果发送失败或者没发送给发送方，则发送方认为失败
                IMSessionManager.sendMsg2User(imMessage.fromId, imCoreMessageACK.toByteArray())

                //发送notify(protobuf)给toId
                val imCoreMessageNotify = IMCoreMessage.newBuilder().apply {
                    version = imCoreMessage.version
                    cmd = imCoreMessage.cmd
                    subCmd = imCoreMessage.subCmd
                    type = 2 //2是notify
                    logId = imCoreMessage.logId
                    seqId = imCoreMessage.seqId
                    bodyLen = body.size
                    bodyData = ByteString.copyFrom(body)
                }.build()
                // 发送给接收方用户失败则使用redis存储离线消息(TODO，应该增加ACK确认机制)
                if (IMSessionManager.sendMsg2User(imMessage.toId, imCoreMessageNotify.toByteArray())) {
                    LOGGER.info("handleReceivedChatBytes： send to ${imMessage.toId} success")
                    return
                }
                val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
                LOGGER.info("handleReceivedChatBytes redis cache uuid ${imCoreMessage.seqId}")
                val score = java.lang.Double.longBitsToDouble(imCoreMessage.seqId)
                LOGGER.info("handleReceivedChatBytes score $score")
                val key = "offline:${imMessage.toId}-${imMessage.fromId}"
                val number = redisCommands.zadd(
                    key,
                    score,
                    String(imCoreMessageNotify.toByteArray(), StandardCharsets.ISO_8859_1)
                )
                redisCommands.expire(key, Duration.ofDays(7))//7天过期
                LOGGER.info("handleReceivedChatBytes redis add key $key, score $score, number $number")
            }
        }

    } catch (e: Exception) {
        LOGGER.info("handleReceivedChatBytes ${e.message}")
    }
}

