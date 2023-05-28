package com.nxg.plugins

import com.nxg.connection.IMManager
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.nxg.connection.IMSession
import com.nxg.im.*
import com.nxg.jwt.JwtConfig
import com.nxg.signaling.*
import com.nxg.sip.CallSession
import com.nxg.sip.callSessionId
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
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
            IMManager.sessions[user.uuid] = imSession

            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()

                try {
                    val imMessage: IMMessage = receivedText.parseMessage()
                    println("chat ${user.username} send $receivedText to ${imMessage.receiver_id} ")
                    IMManager.sendMsg2User(imMessage.receiver_id.toLong(), receivedText)
                } catch (e: Exception) {
                    println(e.localizedMessage)
                }
            }
            println("Removing $imSession!")
            IMManager.sessions.remove(user.uuid)

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
