package com.nxg.im.core.plugins

import com.nxg.im.core.IMCoreMessage
import com.nxg.im.core.session.IMSession
import com.nxg.im.core.session.IMSessionManager
import com.nxg.im.core.signaling.Signaling
import com.nxg.im.core.signaling.SignalingManager
import com.nxg.im.core.signaling.SignalingSession
import com.nxg.im.core.sip.CallSession
import com.nxg.im.core.sip.callSessionId
import com.nxg.im.core.data.bean.IMMessage
import com.nxg.im.core.data.bean.parseIMMessage
import com.nxg.im.core.data.bean.toJson
import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.repository.MessageRepository
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.*
import java.time.Duration

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSockets() {
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
            println("Adding chat user $user!")
            val imSession = IMSession(user, this)
            IMSessionManager.sessions[user.uuid] = imSession

            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedBytes = frame.readBytes()
                        println("chat receivedBytes $receivedBytes")
                        val imCoreMessage = IMCoreMessage.parseFrom(receivedBytes)
                        println("chat imCoreMessage $imCoreMessage")
                        try {
                            val imMessageJson = imCoreMessage.bodyData.toStringUtf8()
                            println("chat imMessageJson $imMessageJson")
                            val imMessage: IMMessage = imMessageJson.parseIMMessage()
                            println("chat ${user.username} send $imMessageJson to ${imMessage.toId}")
                            //保存聊天记录
                            MessageRepository.save(imMessage)
                            //websocket通知相关用户
                            IMSessionManager.sendMsg2User(imMessage.toId, receivedBytes)
                        } catch (e: Exception) {
                            println("chat ${e.message}")
                        }
                    }

                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        println("chat receivedText $receivedText")
                        try {
                            val imMessage: IMMessage = receivedText.parseIMMessage()
                            println("chat imMessage ${imMessage.toJson()} ")
                            println("chat ${user.username} send $receivedText to ${imMessage.toId}")
                            //保存聊天记录
                            MessageRepository.save(imMessage)
                            //websocket通知相关用户
                            IMSessionManager.sendMsg2User(imMessage.toId, receivedText)
                        } catch (e: Exception) {
                            println("chat ${e.message}")
                        }
                    }

                    else -> {
                        continue
                    }
                }

            }
            println("Removing $imSession!")
            IMSessionManager.sessions.remove(user.uuid)

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
            println("Adding push user $user!")
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
                        println(signaling)
                        for (signalingUser in signaling.toUsers) {
                            println("signaling ${user.username} send $text to ${signalingUser.username} ")
                            SignalingManager.sendMsg2User(signalingUser.uuid, text)
                        }
                    } catch (e: Exception) {
                        println("signaling error ${e.message}")
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

