package com.nxg.im.core.data

import com.nxg.im.core.plugins.LOGGER
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger


private class RabbitMQThreadFactory : ThreadFactory {
    private val group: ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        val s = System.getSecurityManager()
        group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
        namePrefix = "RabbitMQ-" + poolNumber.getAndIncrement() + "-thread-"
    }

    override fun newThread(r: Runnable): Thread {
        val t = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0L)
        if (t.isDaemon) {
            t.isDaemon = false
        }
        if (t.priority != 5) {
            t.priority = 5
        }
        return t
    }

    companion object {
        private val poolNumber = AtomicInteger(1)
    }
}


fun newRabbitMQThreadPool(threadFactory: ThreadFactory): ExecutorService {
    val availableProcessors = Runtime.getRuntime().availableProcessors()
    LOGGER.info("newRabbitMQThreadPool $availableProcessors")
    return ThreadPoolExecutor(
        availableProcessors,
        availableProcessors * 2,
        60L,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        threadFactory
    )
}

val RabbitMQDispatcher: CoroutineDispatcher = newRabbitMQThreadPool(RabbitMQThreadFactory()).asCoroutineDispatcher()

val RabbitMQCoroutineScope = CoroutineScope(RabbitMQDispatcher + SupervisorJob())

object RabbitMQClient {

    const val EXCHANGE_CHAT = "exchange_chat"
    const val QUEUE_CHAT = "queue_chat"
    const val ROUTE_KEY_CHAT = "route_key_chat"

    private val connect: com.rabbitmq.client.Connection by lazy {
        val config = HoconApplicationConfig(ConfigFactory.load())
        val host = config.property("ktor.rabbitMQ.host").getString()
        val port = config.property("ktor.rabbitMQ.port").getString().toInt()
        val username = config.property("ktor.rabbitMQ.username").getString()
        val password = config.property("ktor.rabbitMQ.password").getString()
        val virtualHost = config.property("ktor.rabbitMQ.virtualHost").getString()
        val factory = ConnectionFactory()
        factory.host = host
        factory.port = port
        factory.username = username
        factory.password = password
        factory.virtualHost = virtualHost
        factory.newConnection()
    }

    val channel: Channel by lazy {
        connect.createChannel().apply {
            exchangeDeclare(EXCHANGE_CHAT, BuiltinExchangeType.DIRECT, true, false, false, null);
            queueDeclare(QUEUE_CHAT, true, false, false, null)
            queueBind(QUEUE_CHAT, EXCHANGE_CHAT, ROUTE_KEY_CHAT)
        }
    }
}