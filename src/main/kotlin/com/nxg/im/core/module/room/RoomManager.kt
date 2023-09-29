package com.nxg.im.core.module.room

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

interface IRoomManager{
    /**
     * 创建房间
     */
    fun createRoom():Long



}
object RoomManager {

    val rooms = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

}