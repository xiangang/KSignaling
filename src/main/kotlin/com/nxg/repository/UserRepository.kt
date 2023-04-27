package com.nxg.repository

import com.nxg.data.db.KSignalingDatabase.database
import com.nxg.data.entity.User
import com.nxg.data.entity.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UserRepository {

    fun findById(id: Long): User? {
        return transaction(database) {
            UsersTable.select { UsersTable.id eq id }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun findByUsername(username: String): User? {
        return transaction(database) {
            UsersTable.select { UsersTable.username eq username }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun save(user: User) {
        transaction(database) {
            UsersTable.insert {
                it[id] = user.id
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
            id = row[UsersTable.id].value,
            username = row[UsersTable.username],
            password = row[UsersTable.password],
            salt = row[UsersTable.salt],
            email = row[UsersTable.email],
            phone = row[UsersTable.phone],
            nickname = row[UsersTable.nickname],
            avatar = row[UsersTable.avatar],
            gender = row[UsersTable.gender],
            birthday = row[UsersTable.birthday]?.toLocalDate(),
            country = row[UsersTable.country],
            province = row[UsersTable.province],
            city = row[UsersTable.city],
            address = row[UsersTable.address],
            status = row[UsersTable.status].toInt(),
            createTime = row[UsersTable.create_time].toLocalDateTime(),
            updateTime = row[UsersTable.update_time].toLocalDateTime(),
        )
    }
}