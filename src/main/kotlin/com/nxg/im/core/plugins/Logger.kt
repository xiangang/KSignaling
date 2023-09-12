package com.nxg.im.core.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.util.logging.*
import org.slf4j.event.Level

val LOGGER = KtorSimpleLogger("com.nxg.im.core.plugins.RequestTracePlugin")

val RequestTracePlugin = createRouteScopedPlugin("RequestTracePlugin", { }) {
    onCall { call ->
        LOGGER.trace("Processing call: ${call.request.uri}")
    }
}

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}
