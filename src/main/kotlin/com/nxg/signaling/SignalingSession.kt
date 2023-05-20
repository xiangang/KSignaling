package com.nxg.signaling

import com.nxg.data.entity.User
import io.ktor.websocket.*
import java.util.UUID

class SignalingSession(val user: User, val session: DefaultWebSocketSession)
