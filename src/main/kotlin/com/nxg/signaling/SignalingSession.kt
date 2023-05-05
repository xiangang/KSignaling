package com.nxg.signaling

import com.nxg.data.entity.User
import io.ktor.websocket.*

class SignalingSession(val user: User, val session: DefaultWebSocketSession)
