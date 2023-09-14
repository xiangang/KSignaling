package com.nxg.im.core.session

import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.sip.CallSession
import io.ktor.websocket.*

object SIPSessionManager {


    val sipSessions = mutableMapOf<String, CallSession>()

}