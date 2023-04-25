package com.nxg.repository

import com.nxg.data.entity.User
import com.nxg.data.entity.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object UserRepository {
    fun findById(id: UUID): User? {
        return transaction {
            UsersTable.select { UsersTable.id eq id }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun findByUsername(username: String): User? {
        return transaction {
            UsersTable.select { UsersTable.username eq username }
                .mapNotNull { toUser(it) }
                .singleOrNull()
        }
    }

    fun save(user: User) {
        transaction {
            UsersTable.insert {
                it[id] = user.id
                it[username] = user.username
                it[passwordHash] = user.passwordHash
            }
        }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UsersTable.id].value,
            username = row[UsersTable.username],
            passwordHash = row[UsersTable.passwordHash]
        )
    }
}