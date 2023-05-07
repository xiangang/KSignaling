package com.nxg

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.nxg.plugins.*
import com.nxg.utils.HoconUtils
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

fun main() {
    val config = HoconUtils.config
    val host = config.property("ktor.deployment.http.host").getString()
    val port = config.property("ktor.deployment.http.port").getString()
    embeddedServer(Netty, port = port.toInt(), host = host, module = Application::module)
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
