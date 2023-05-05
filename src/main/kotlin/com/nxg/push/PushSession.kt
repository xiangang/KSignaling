package com.nxg.push

import io.ktor.websocket.*
import java.util.concurrent.atomic.*

class PushSession(val userId: Long, val session: DefaultWebSocketSession)