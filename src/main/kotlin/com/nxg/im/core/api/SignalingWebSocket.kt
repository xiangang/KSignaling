package com.nxg.im.core.api

import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.signaling.Signaling
import com.nxg.im.core.signaling.SignalingManager
import com.nxg.im.core.signaling.SignalingSession
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Route.signalingWebSocket() {
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
        com.nxg.im.core.plugins.LOGGER.info("Adding push user $user!")
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
                    com.nxg.im.core.plugins.LOGGER.info("signaling")
                    for (signalingUser in signaling.toUsers) {
                        com.nxg.im.core.plugins.LOGGER.info("signaling ${user.username} send $text to ${signalingUser.username} ")
                        SignalingManager.sendMsg2User(signalingUser.uuid, text)
                    }
                } catch (e: Exception) {
                    com.nxg.im.core.plugins.LOGGER.info("signaling error ${e.message}")
                }
            }
        }

        // 连接关闭时，从映射中删除 SignalingSession 对象
        SignalingManager.sessions.remove(user.uuid)
    }

}