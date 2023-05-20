package com.nxg.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.jodatime.datetime

object FriendTable : LongIdTable(name = "friend") {
    val user_id = long("user_id")
    val friend_id = long("friend_id")
    val relation_type = integer("relation_type").default(0)
    val relation_status = integer("relation_status").default(0)
    val remark = varchar("remark", 255).default("")
    val permission = integer("permission").default(0)
    val group_id = integer("group_id").default(0)
    val latest_dynamic = varchar("latest_dynamic", 255).nullable()
    val is_recommend = integer("is_recommend").default(0)
    val create_time = datetime("create_time")
    val update_time = datetime("update_time")
}