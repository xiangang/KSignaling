package com.nxg.im.core.module.signaling

import com.nxg.im.core.data.entity.User
import io.ktor.websocket.*

class SignalingSession(val user: User, val session: DefaultWebSocketSession)
