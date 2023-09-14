package com.nxg.im.core.api

import com.nxg.im.core.data.entity.UserTable
import com.nxg.im.core.data.entity.toSimpleUser
import com.nxg.im.core.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


fun Route.userAPI() {
    get("$API_V1/me") {
        val user = call.user
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to user.toSimpleUser()
            )
        )
    }

    // 获取所有用户
    get("$API_V1/users") {
        val users = transaction {
            UserTable.selectAll().map { UserRepository.toUser(it).toSimpleUser() }
        }
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to users
            )
        )
    }
}
