package com.nxg.im.core.session

import com.nxg.im.core.data.entity.User
import io.ktor.websocket.*

class IMSession(val user: User, val session: DefaultWebSocketSession) {

    val name = user.username

    override fun toString(): String {
        return super.toString() + user.toString()
    }
}