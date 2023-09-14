package com.nxg.im.core.plugins

import com.nxg.im.core.api.SIPWebSocket
import com.nxg.im.core.api.chatWebSocket
import com.nxg.im.core.api.signalingWebSocket
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.protobuf.ProtoBuf
import java.time.Duration

fun Application.configureSockets() {

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(ProtoBuf)
    }

    routing {

        chatWebSocket()

        signalingWebSocket()

        SIPWebSocket()

    }

}

