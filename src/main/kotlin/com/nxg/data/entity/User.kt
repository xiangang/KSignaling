package com.nxg.data.entity

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

data class User(
    val id: Long,
    val username: String,
    val password: String,
    val salt: String,
    val email: String? = "",
    val phone: String? = "",
    val nickname: String? = "",
    val avatar: String? = "",
    val gender: Byte? = null,
    val birthday: LocalDate? = null,
    val country: String? = "",
    val province: String? = "",
    val city: String? = "",
    val address: String? = "",
    val status: Int = 0,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)