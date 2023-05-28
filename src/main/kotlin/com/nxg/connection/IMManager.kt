package com.nxg.connection

import io.ktor.websocket.*

object IMManager {

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