package com.nxg.connection

import com.nxg.data.entity.User
import io.ktor.websocket.*

class IMSession(val user: User, val session: DefaultWebSocketSession) {

    val name = user.username
}