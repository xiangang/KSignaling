package com.nxg.im.core.api

import com.google.protobuf.ByteString
import com.nxg.im.core.IMCoreMessage
import com.nxg.im.core.middleware.rabbitmq.RabbitMQClient
import com.nxg.im.core.middleware.rabbitmq.RabbitMQCoroutineScope
import com.nxg.im.core.data.bean.ChatMessage
import com.nxg.im.core.data.bean.parseChatMessage
import com.nxg.im.core.middleware.redis.KSignalingRedisClient
import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.repository.MessageRepository
import com.nxg.im.core.module.session.IMSession
import com.nxg.im.core.module.session.IMSessionManager
import com.nxg.im.core.module.signaling.Signaling
import com.nxg.im.core.module.signaling.parseSignaling
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.time.Duration

fun Route.chatWebSocket() {
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
    //聊天管理
    webSocket("/chat") {
        val user = getUserByAuthorization()
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is invalid or has expired"))
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
}

suspend fun handleReceivedChatBytes(receivedBytes: ByteArray) {
    LOGGER.info("handleReceivedChatBytes receivedBytes $receivedBytes")
    val imCoreMessage = IMCoreMessage.parseFrom(receivedBytes)
    LOGGER.info("handleReceivedChatBytes imCoreMessage $imCoreMessage")
    try {
        val bodyDataJson = imCoreMessage.bodyData.toStringUtf8()
        LOGGER.info("handleReceivedChatBytes bodyDataJson $bodyDataJson")
        //bodyData存的是json
        when (imCoreMessage.cmd) {
            "chat" -> {
                val chatMessage: ChatMessage = bodyDataJson.parseChatMessage()
                LOGGER.info("handleReceivedChatBytes ${chatMessage.fromId} send $bodyDataJson to ${chatMessage.toId}")
                //消息去重，避免发送方重复发送
                if (!MessageRepository.isChatMessageExist(imCoreMessage.seqId)) {
                    val body = imCoreMessage.bodyData.toByteArray()
                    //保存聊天记录，一旦落库，就认为消息接收成功，此时可以发送ACK给发送方客户端
                    val id = MessageRepository.save(imCoreMessage.seqId, chatMessage)
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
                        IMSessionManager.sendMsg2User(chatMessage.fromId, imCoreMessageACK.toByteArray())

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
                        if (IMSessionManager.sendMsg2User(chatMessage.toId, imCoreMessageNotify.toByteArray())) {
                            LOGGER.info("handleReceivedChatBytes： send to ${chatMessage.toId} success")
                            return
                        }
                        val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
                        LOGGER.info("handleReceivedChatBytes redis cache uuid ${imCoreMessage.seqId}")
                        val score = java.lang.Double.longBitsToDouble(imCoreMessage.seqId)
                        LOGGER.info("handleReceivedChatBytes score $score")
                        val key = "offline:${chatMessage.toId}-${chatMessage.fromId}"
                        val number = redisCommands.zadd(
                            key,
                            score,
                            String(imCoreMessageNotify.toByteArray(), StandardCharsets.ISO_8859_1)
                        )
                        redisCommands.expire(key, Duration.ofDays(7))//7天过期
                        LOGGER.info("handleReceivedChatBytes redis add key $key, score $score, number $number")
                    }
                }
            }

            "signaling" -> {
                val signaling: Signaling = bodyDataJson.parseSignaling()
                LOGGER.info("handleReceivedSignalingBytes ${signaling.fromId} send $bodyDataJson to ${signaling.participants}")
                val bodyByteArray = imCoreMessage.bodyData.toByteArray()
                //发送acknowledge(protobuf)给fromId
                IMCoreMessage.newBuilder().apply {
                    version = imCoreMessage.version
                    cmd = imCoreMessage.cmd
                    subCmd = imCoreMessage.subCmd
                    type = 1 // 1是acknowledge
                    logId = imCoreMessage.logId
                    seqId = imCoreMessage.seqId
                    bodyLen = bodyByteArray.size
                    bodyData = ByteString.copyFrom(bodyByteArray)
                }.build().also {
                    //如果发送失败或者没发送给发送方，则发送方认为失败
                    LOGGER.info("handleReceivedSignalingBytes ${signaling.fromId} ack to ${signaling.fromId}")
                    IMSessionManager.sendMsg2User(signaling.fromId, it.toByteArray())
                }

                //发送notify(protobuf)给toId
                val imCoreMessageNotify = IMCoreMessage.newBuilder().apply {
                    version = imCoreMessage.version
                    cmd = imCoreMessage.cmd
                    subCmd = imCoreMessage.subCmd
                    type = 2 //2是notify
                    logId = imCoreMessage.logId
                    seqId = imCoreMessage.seqId
                    bodyLen = bodyByteArray.size
                    bodyData = ByteString.copyFrom(bodyByteArray)
                }.build()
                signaling.participants.forEach {
                    if (it != signaling.fromId) {
                        LOGGER.info("handleReceivedSignalingBytes ${signaling.fromId} send to $it")
                        IMSessionManager.sendMsg2User(it, imCoreMessageNotify.toByteArray())
                    }
                }
            }
        }


    } catch (e: Exception) {
        LOGGER.info("handleReceivedChatBytes ${e.message}")
    }
}
