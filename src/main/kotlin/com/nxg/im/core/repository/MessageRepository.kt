package com.nxg.im.core.repository

import com.nxg.im.core.data.bean.IMMessage
import com.nxg.im.core.data.bean.toJson
import com.nxg.im.core.data.db.KSignalingDatabase
import com.nxg.im.core.data.entity.MessageTable
import com.nxg.im.core.utils.SnowflakeUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

object MessageRepository {

    fun save(message: IMMessage):Long {
        return transaction(KSignalingDatabase.database) {
            MessageTable.insertAndGetId {
                it[uuid] = SnowflakeUtils.snowflake.nextId()
                it[from_id] = message.fromId
                it[to_id] = message.toId
                it[chat_type] = message.chatType
                it[content] = message.content.toJson()
            }.value
        }
    }
}