package com.nxg.im.core.session

import io.ktor.websocket.*

object IMSessionManager {

    val sessions = mutableMapOf<Long, IMSession>()

    /**
     * 发送消息给指定用户
     */
    suspend fun sendMsg2User(uuid: Long, text: String) {
        sessions[uuid]?.let {
            it.session.send(text)
            println("chat send $text to ${it.user.username} ")
        }

    }
}