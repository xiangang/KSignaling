package com.nxg.im.core.room

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object RoomManager {

    val rooms = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

}