package com.nxg.im.core.api

import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.module.sip.CallSession
import com.nxg.im.core.module.sip.SIPSessionManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach

fun Route.SIPWebSocket() {
    webSocket("/sip") {
        val user = getUserByAuthorization()
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is invalid or has expired"))
            return@webSocket
        }
        val callId = user.uuid.toString()
        val callSession = CallSession(callId, this)
        SIPSessionManager.sipSessions[callId] = callSession
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
            SIPSessionManager.sipSessions.remove(callId)
            callSession.close()
        }
    }
}