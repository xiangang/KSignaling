package com.nxg.plugins

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import java.util.*
import kotlin.collections.LinkedHashSet
import com.nxg.connection.Connection
import com.nxg.data.entity.SimpleUser
import com.nxg.data.entity.toSimpleUser
import com.nxg.jwt.JwtConfig
import com.nxg.signaling.SignalingManager
import com.nxg.signaling.SignalingSession
import com.nxg.signaling.Signaling
import com.nxg.sip.CallSession
import com.nxg.sip.callSessionId
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
        //聊天管理
        webSocket("/chat}") {
            val userId = call.parameters["userId"]
            println("Chat to user $userId!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val textWithUsername = "[${thisConnection.name}]: $receivedText"
                    connections.forEach {
                        it.session.send(textWithUsername)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
        //房间管理
        webSocket("/room/{roomId}") { // websocketSession
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
        //信令管理
        webSocket("/signaling") {
            val token = call.request.headers["Authorization"]
            if (token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token is null"))
                return@webSocket
            }
            val user = JwtConfig.getUserByToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            // websocketSession
            println("Adding push user $user!")
            val newSignalingSession = SignalingSession(user, this)
            SignalingManager.signalingSessions += newSignalingSession
            val json = Json.encodeToString(SimpleUser.serializer(), user.toSimpleUser())
            //send(json)
            //测试发现不加这个代码websocket会立刻断开
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                    try {
                        val signaling = Json.decodeFromString(Signaling.serializer(), text)
                        println(signaling)
                        for (signalingSession in SignalingManager.signalingSessions) {
                            if (signaling.toUsers.contains(signalingSession.user.uuid.toString())) {
                                println("signaling ${user.username} send $text to ${signalingSession.user.username} ")
                                signalingSession.session.send(text)
                            }
                        }
                        when (signaling.action) {
                            "call" -> {


                            }

                            "answer" -> {

                            }
                        }
                    } catch (e: Exception) {
                        println("signaling ${e.message}")
                    }
                }
            }
        }

        val callSessions = mutableMapOf<String, CallSession>()

        webSocket("/sip") {
            val callId = callSessionId()
            val callSession = CallSession(callId, this)
            callSessions[callId] = callSession
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
                callSessions.remove(callId)
                callSession.close()
            }
        }

    }

}
