package com.nxg.im.core.api

import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.module.signaling.Signaling
import com.nxg.im.core.module.signaling.SignalingManager
import com.nxg.im.core.module.signaling.SignalingSession
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Route.signalingWebSocket() {
    //信令管理
    webSocket("/signaling") {
        val user = getUserByAuthorization()
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is invalid or has expired"))
            return@webSocket
        }
        LOGGER.info("signalingWebSocket push user $user!")
        // 将 SignalingSession 对象与用户 ID 相关联
        val newSignalingSession = SignalingSession(user, this)
        SignalingManager.sessions[user.uuid] = newSignalingSession
        // 处理从客户端发送的消息
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                if (text.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "signalingWebSocket Client said BYE"))
                }
                try {
                    val signaling = Json.decodeFromString(Signaling.serializer(), text)
                    LOGGER.info("signalingWebSocket")
                    for (signalingUser in signaling.participants) {
                        LOGGER.info("signalingWebSocket ${user.username} send $text to $signalingUser ")
                        SignalingManager.sendMsg2User(signalingUser, text)
                    }
                } catch (e: Exception) {
                    LOGGER.info("signalingWebSocket error ${e.message}")
                }
            }
        }

        // 连接关闭时，从映射中删除 SignalingSession 对象
        SignalingManager.sessions.remove(user.uuid)
    }

}