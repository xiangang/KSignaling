package com.nxg.im.core.session

import com.nxg.im.core.plugins.LOGGER
import io.ktor.websocket.*

object IMSessionManager {

    val sessions = mutableMapOf<Long, IMSession>()

    /**
     * 发送消息给指定用户
     */
    suspend fun sendMsg2User(uuid: Long, byteArray: ByteArray): Boolean {
        return sessions[uuid]?.let {
            it.session.send(byteArray)
             LOGGER.info("IMSessionManager send $byteArray to ${it.user.username} ")
            true
        } ?: let {
             LOGGER.info("IMSessionManager send $byteArray failed, user $uuid offline !")
            false
        }

    }
}