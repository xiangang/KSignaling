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
            println("IMSessionManager send $text to ${it.user.username} ")
        }

    }

    suspend fun sendMsg2User(uuid: Long, byteArray: ByteArray): Boolean {
        return sessions[uuid]?.let {
            it.session.send(byteArray)
            println("IMSessionManager send $byteArray to ${it.user.username} ")
            true
        } ?: let {
            println("IMSessionManager send $byteArray failed, user $uuid offline !")
            false
        }

    }
}