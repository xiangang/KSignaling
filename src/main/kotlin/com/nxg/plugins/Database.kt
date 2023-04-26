package com.nxg.plugins

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    val url = config.property("ktor.database.url").getString()
    val driver = config.property("ktor.database.driver").getString()
    val user = config.property("ktor.database.user").getString()
    val password = config.property("ktor.database.password").getString()
    Database.connect(url, driver, user, password)
}