package com.nxg.im.core.api

import com.nxg.im.core.sip.CallSession
import com.nxg.im.core.sip.callSessionId
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach

fun Route.SIPWebSocket() {
    webSocket("/sip") {
        val callId = callSessionId()
        val callSession = CallSession(callId, this)
        com.nxg.im.core.session.SIPSessionManager.sipSessions[callId] = callSession
        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    callSession.handleMessage(message)
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // Client disconnected
        } finally {
            com.nxg.im.core.session.SIPSessionManager.sipSessions.remove(callId)
            callSession.close()
        }
    }
}