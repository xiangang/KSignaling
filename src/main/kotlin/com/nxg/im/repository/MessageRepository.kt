package com.nxg.im.repository

import com.nxg.im.data.bean.IMMessage
import com.nxg.im.data.bean.toJson
import com.nxg.im.data.db.KSignalingDatabase
import com.nxg.im.data.entity.MessageTable
import com.nxg.im.utils.SnowflakeUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object MessageRepository {

    fun save(message: IMMessage) {
        transaction(KSignalingDatabase.database) {
            MessageTable.insert {
                it[uuid] = SnowflakeUtils.snowflake.nextId()
                it[from_id] = message.from_id
                it[to_id] = message.to_id
                it[chat_type] = message.chat_type
                it[content] = message.content.toJson()
            }
        }
    }
}