package com.nxg.repository

import com.nxg.data.db.KSignalingDatabase.database
import com.nxg.data.entity.User
import com.nxg.data.entity.UserTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UserRepository {

    fun findById(id: Long): User? {
        return transaction(database) {
            UserTable.select { UserTable.id eq id }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun findByUUId(uuid: Long): User? {
        return transaction(database) {
            UserTable.select { UserTable.uuid eq uuid }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun findByUsername(username: String): User? {
        return transaction(database) {
            UserTable.select { UserTable.username eq username }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun save(user: User) {
        transaction(database) {
            UserTable.insert {
                it[id] = user.id
                it[uuid] = user.uuid
                it[username] = user.username
                it[password] = user.password
                it[salt] = user.salt
                it[email] = user.email
                it[phone] = user.phone
                it[nickname] = user.nickname
                it[avatar] = user.avatar
                it[gender] = user.gender
                it[birthday] = user.birthday?.toDateTimeAtStartOfDay()
                it[country] = user.country
                it[province] = user.province
                it[city] = user.city
                it[address] = user.address
                it[province] = user.province
                it[status] = user.status.toByte()
                it[create_time] = user.createTime.toDateTime()
                it[update_time] = user.updateTime.toDateTime()
            }
        }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UserTable.id].value,
            uuid = row[UserTable.uuid],
            username = row[UserTable.username],
            password = row[UserTable.password],
            salt = row[UserTable.salt],
            email = row[UserTable.email],
            phone = row[UserTable.phone],
            nickname = row[UserTable.nickname],
            avatar = row[UserTable.avatar],
            gender = row[UserTable.gender],
            birthday = row[UserTable.birthday]?.toLocalDate(),
            country = row[UserTable.country],
            province = row[UserTable.province],
            city = row[UserTable.city],
            address = row[UserTable.address],
            status = row[UserTable.status].toInt(),
            createTime = row[UserTable.create_time].toLocalDateTime(),
            updateTime = row[UserTable.update_time].toLocalDateTime(),
        )
    }
}