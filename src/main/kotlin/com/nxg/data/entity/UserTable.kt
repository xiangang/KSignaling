package com.nxg.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable

object UsersTable : UUIDTable() {
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
}