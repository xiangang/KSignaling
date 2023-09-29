package com.nxg.im.core.middleware.redis

import com.nxg.im.core.plugins.LOGGER
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import org.joda.time.LocalDateTime

object KSignalingRedisClient {
    val redisClient: RedisClient by lazy {
        val config = HoconApplicationConfig(ConfigFactory.load())
        val url = config.property("ktor.redis.url").getString()
        val port = config.property("ktor.redis.port").getString()
        val password = config.property("ktor.redis.password").getString()
        val redisURI = RedisURI.Builder.redis(url, port.toInt()).withPassword(password.toCharArray()).build()
        RedisClient.create(redisURI)
    }
   val redisClientConnection: StatefulRedisConnection<String, String> by lazy {
       redisClient.connect()
    }

    fun start() {
        val connection = redisClient.connect()
        connection.sync().set("start_time", LocalDateTime().toString())
        connection.sync().set("start_time_joda", LocalDateTime().toString())
        connection.sync().set("start_time_java", java.time.LocalDateTime.now().toString())
        val startTime = connection.sync().get("start_time")
        val startTimeJoda = connection.sync().get("start_time_joda")
        val startTimeJava = connection.sync().get("start_time_java")
         LOGGER.info("start_time: $startTime")
         LOGGER.info("start_time_joda: $startTimeJoda")
         LOGGER.info("start_time_java: $startTimeJava")
        connection.close()
    }
}