package com.nxg.im.core

import com.nxg.im.core.data.db.KSignalingDatabase
import com.nxg.im.core.data.entity.*
import com.nxg.im.core.data.redis.KSignalingRedisClient
import com.nxg.im.core.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.nxg.im.core.utils.HoconUtils
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetAddress

fun main() {
    val ip = InetAddress.getLocalHost().hostAddress
    println("KSignaling IP：$ip")
    KSignalingDatabase.database
    KSignalingRedisClient.start()
    transaction {
        SchemaUtils.create(UserTable, FriendTable, GroupTable, GroupMemberTable, GroupMessageTable, MessageTable)
    }
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
