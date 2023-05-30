package com.nxg.im.data.entity

data class Friend(
    val user: SimpleUser,
    val relationType: Int,
    val relationStatus: Int,
    val remark: String,
    val permission: Int = 0,
    val groupId: Int = 0,
    val latestDynamic: String,
    val isRecommend: Int,
    val createTime: String,
    val updateTime: String
)