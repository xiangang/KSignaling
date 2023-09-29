package com.nxg.im.core.api

import com.nxg.im.core.config.jwt.JwtConfig
import com.nxg.im.core.data.entity.User
import com.nxg.im.core.data.entity.toSimpleUser
import com.nxg.im.core.utils.SnowflakeUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)

class UserPrincipal(val user: User) : Principal

val ApplicationCall.user: User
    get() = authentication.principal<UserPrincipal>()!!.user

fun Route.authAPI() {
    post("$API_V1/register") {
        val request = call.receive<RegisterRequest>()
        val user = com.nxg.im.core.repository.UserRepository.findByUsername(request.username)
        if (user != null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to HttpStatusCode.BadRequest.value,
                    "message" to "Username already exists",
                    "data" to null
                )
            )
        } else {
            val salt: String = com.nxg.im.core.utils.PasswordUtils.generateSalt(16)
            val passwordHash = com.nxg.im.core.utils.PasswordUtils.hashPassword(request.password, salt)
            val now = org.joda.time.LocalDateTime.now()
            val newUser = User(
                0,
                SnowflakeUtils.snowflake.nextId(),
                request.username,
                passwordHash,
                salt,
                createTime = now,
                updateTime = now
            )
            com.nxg.im.core.repository.UserRepository.save(newUser)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.OK.value,
                    "message" to HttpStatusCode.OK.description,
                    "data" to newUser.toSimpleUser()
                )
            )
        }
    }
    post("$API_V1/login") {
        val request = call.receive<LoginRequest>()
        val user = com.nxg.im.core.repository.UserRepository.findByUsername(request.username)
        if (user == null || !com.nxg.im.core.utils.PasswordUtils.verifyPassword(
                request.password,
                user.salt,
                user.password
            )
        ) {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf(
                    "code" to HttpStatusCode.Unauthorized.value,
                    "message" to "Invalid username or password",
                    "data" to null
                )
            )
        } else {
            val token = JwtConfig.generateToken(user)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.OK.value,
                    "message" to HttpStatusCode.OK.description,
                    "data" to mapOf("token" to token, "user" to user.toSimpleUser())
                )
            )
        }
    }
}
