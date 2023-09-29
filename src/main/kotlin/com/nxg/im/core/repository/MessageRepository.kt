package com.nxg.im.core.repository

import com.nxg.im.core.data.bean.ChatMessage
import com.nxg.im.core.data.bean.toJson
import com.nxg.im.core.data.db.KSignalingDatabase
import com.nxg.im.core.data.entity.MessageTable
import com.nxg.im.core.middleware.redis.KSignalingRedisClient
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

object MessageRepository {

    fun isChatMessageExist(uuid: Long): Boolean {
        return transaction(KSignalingDatabase.database) {
            //先查缓存
            val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
            val key = "chat:$uuid"
            if (redisCommands.get(key) != null) {
                return@transaction true
            }
            //查不到缓存再查数据库
            MessageTable.select { MessageTable.uuid eq uuid }.singleOrNull()?.let { true } ?: false
        }
    }

    fun save(seqId: Long, chatMessage: ChatMessage): Long {
        return transaction(KSignalingDatabase.database) {
            val id = MessageTable.insertAndGetId {
                it[uuid] = seqId
                it[from_id] = chatMessage.fromId
                it[to_id] = chatMessage.toId
                it[chat_type] = chatMessage.chatType
                it[content] = chatMessage.content.toJson()
            }.value
            //缓存
            val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
            val key = "chat:$seqId"
            redisCommands.set(key, chatMessage.toJson())
            redisCommands.expire(key, Duration.ofMinutes(10))
            id
        }
    }
}