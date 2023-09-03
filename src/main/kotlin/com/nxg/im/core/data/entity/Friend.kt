package com.nxg.im.core.data.entity

data class Friend(
    val userId: Long,
    val friendId: Long,
    val username: String,
    val email: String? = "",
    val phone: String? = "",
    val nickname: String? = "",
    val avatar: String? = "",
    val gender: Int? = null,
    val birthday: String? = null,
    val country: String? = "",
    val province: String? = "",
    val city: String? = "",
    val address: String? = "",
    val status: Int = 0,
    val relationType: Int,
    val relationStatus: Int,
    val remark: String,
    val permission: Int = 0,
    val groupId: Int = 0,
    val latestDynamic: String,
    val isRecommend: Int,
    val createTime: Long,
    val updateTime: Long
)