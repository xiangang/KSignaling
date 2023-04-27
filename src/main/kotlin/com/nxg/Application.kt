package com.nxg

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.nxg.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    val port = config.property("ktor.deployment.http.port").getString()
    embeddedServer(Netty, port = port.toInt(), host = "127.0.0.1", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureSockets()
    configureRouting()
}
