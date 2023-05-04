package com.nxg.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.jodatime.date
import org.jetbrains.exposed.sql.jodatime.datetime

object FriendTable : LongIdTable(name = "friend") {
    val user_id = long("user_id")
    val friend_id = long("friend_id")
    val relation_type = integer("relation_type")
    val relation_status = integer("relation_status")
    val remark = varchar("remark", 255).nullable()
    val permission = integer("permission").default(1)
    val group_id = integer("group_id")
    val latest_dynamic = varchar("latest_dynamic", 255).nullable()
    val is_recommend = integer("is_recommend").default(0)
    val create_time = datetime("create_time")
    val update_time = datetime("update_time")
}