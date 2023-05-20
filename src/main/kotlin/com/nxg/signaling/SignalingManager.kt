package com.nxg.signaling

import com.nxg.data.entity.User
import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

object SignalingManager {

    val sessions = mutableMapOf<Long, SignalingSession>()

    /**
     * 发送消息给指定用户
     */
    suspend fun sendMsg2User(uuid: Long, text: String) {
        sessions[uuid]?.session?.send(text)
    }
}