package com.nxg.signaling

import io.ktor.websocket.*

object SignalingManager {

    val sessions = mutableMapOf<Long, SignalingSession>()

    /**
     * 发送消息给指定用户
     */
    suspend fun sendMsg2User(uuid: Long, text: String) {
        sessions[uuid]?.session?.send(text)
    }
}