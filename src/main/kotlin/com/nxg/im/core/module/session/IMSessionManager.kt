package com.nxg.im.core.module.session

import com.nxg.im.core.plugins.LOGGER
import io.ktor.websocket.*

object IMSessionManager {

    val sessions = mutableMapOf<Long, IMSession>()

    /**
     * 发送消息给指定用户
     */
    suspend fun sendMsg2User(acid: Long, byteArray: ByteArray): Boolean {
        return sessions[acid]?.let {
            it.session.send(byteArray)
             LOGGER.info("IMSessionManager send $byteArray to ${it.user.username} ")
            true
        } ?: let {
             LOGGER.info("IMSessionManager send $byteArray failed, user $acid offline !")
            false
        }

    }
}