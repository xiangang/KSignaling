package com.nxg.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    routing {
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
