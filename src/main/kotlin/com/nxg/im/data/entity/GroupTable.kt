package com.nxg.im.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.jodatime.datetime


object GroupTable : LongIdTable(name = "group") {
    val groupName = varchar("group_name", 50).uniqueIndex()
    val creatorId = reference("creator_id", UserTable.uuid)
    val createTime = datetime("create_time").defaultExpression(CurrentDateTime)
}

object GroupMemberTable : LongIdTable(name = "group_member") {
    val groupId = reference("group_id", GroupTable.id)
    val userId = reference("user_id", UserTable.uuid)
}

object GroupMessageTable : LongIdTable(name = "group_message") {
    val groupId = reference("group_id", GroupTable.id)
    val senderId = reference("sender_id", UserTable.uuid)
    val messageContent = varchar("message_content", 255)
    val sendTime = datetime("send_time").defaultExpression(CurrentDateTime)
}