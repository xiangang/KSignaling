package com.nxg.im.core.session

import com.nxg.im.data.entity.User
import io.ktor.websocket.*

class IMSession(val user: User, val session: DefaultWebSocketSession) {

    val name = user.username
}