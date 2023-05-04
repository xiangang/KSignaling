package com.nxg.data.entity

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.jodatime.date
import org.jetbrains.exposed.sql.jodatime.datetime

object UserTable : LongIdTable(name = "user") {
    val uuid = long("uuid").uniqueIndex()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 128)
    val email = varchar("email", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val salt = varchar("salt", 32)
    val nickname = varchar("nickname", 50).nullable()
    val avatar = varchar("avatar", 200).nullable()
    val gender = byte("gender").nullable()
    val birthday = date("birthday").nullable()
    val country = varchar("country", 50).nullable()
    val province = varchar("province", 50).nullable()
    val city = varchar("city", 50).nullable()
    val address = varchar("address", 200).nullable()
    val status = byte("status").default(0)
    val create_time = datetime("create_time")
    val update_time = datetime("update_time")
}