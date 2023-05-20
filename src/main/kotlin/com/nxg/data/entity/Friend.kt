package com.nxg.data.entity

import org.joda.time.LocalDateTime

data class Friend(
    val id: Long,
    val userId: Long,
    val friendId: Long,
    val relationType: Int,
    val relationStatus: Int,
    val remark: String,
    val permission: Int = 0,
    val groupId: Int = 0,
    val latestDynamic: String,
    val isRecommend: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)