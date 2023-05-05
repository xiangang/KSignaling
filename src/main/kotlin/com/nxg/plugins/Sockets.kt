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
        webSocket("/chat/{roomId}") {
            val roomId = call.parameters["roomId"]
            println("Adding user in room $roomId!")
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
        webSocket("/ws") { // websocketSession
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
        webSocket("/friend") { // websocketSession
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
            send(json)
            //outgoing.send(Frame.Text("Welcome ${user.username}"))
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

    }

}
