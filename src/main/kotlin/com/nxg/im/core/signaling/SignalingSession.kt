package com.nxg.im.core.signaling

import com.nxg.im.data.entity.User
import io.ktor.websocket.*

class SignalingSession(val user: User, val session: DefaultWebSocketSession)