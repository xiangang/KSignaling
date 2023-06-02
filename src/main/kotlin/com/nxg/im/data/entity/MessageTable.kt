package com.nxg.im.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.jodatime.datetime

/**
 * 聊天消息表
 */
object MessageTable : LongIdTable(name = "message") {
    val uuid = long("uuid").uniqueIndex()
    val from_id = reference("from_id", UserTable.uuid)
    val to_id = long("to_id")//单聊时是user_id，群聊是group_id
    val chat_type = integer("chat_type")//0是单聊，1是群聊
    val content = text("content")
    val createTime = datetime("create_time").defaultExpression(CurrentDateTime)
}
