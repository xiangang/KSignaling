package com.nxg.im.core.data.entity

import kotlinx.serialization.Serializable
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

data class User(
    val id: Long,
    val uuid: Long,
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

const val patternYmdHms = "yyyy-MM-dd HH:mm:ss"
const val patternYmd = "yyyy-MM-dd"
fun User.toSimpleUser(): SimpleUser {
    return SimpleUser(
        id = id,
        uuid = uuid,
        username = username,
        email = email,
        phone = phone,
        nickname = nickname,
        avatar = avatar,
        gender = gender?.toInt(),
        birthday = birthday?.toString(patternYmd),
        country = country,
        province = province,
        address = address,
        status = status,
        createTime = createTime.toString(patternYmdHms),
        updateTime = updateTime.toString(patternYmdHms),
    )
}

@Serializable
data class SimpleUser(
    val id: Long,
    val uuid: Long,
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
    val createTime: String,
    val updateTime: String
)