package com.nxg.plugins

import com.nxg.data.entity.User
import com.nxg.jwt.JwtConfig
import com.nxg.repository.UserRepository
import com.nxg.utils.PasswordUtils.hashPassword
import com.nxg.utils.PasswordUtils.verifyPassword
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.*

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("KSignaling")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    routing {
        //swaggerUI(path = "openapi")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    routing {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val user = UserRepository.findByUsername(request.username)
            if (user != null) {
                call.respond(HttpStatusCode.BadRequest, "Username already exists")
            } else {
                val passwordHash = hashPassword(request.password)
                val newUser = User(UUID.randomUUID(), request.username, passwordHash)
                UserRepository.save(newUser)
                call.respond(HttpStatusCode.Created, newUser)
            }
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = UserRepository.findByUsername(request.username)
            if (user == null || !verifyPassword(request.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
            } else {
                val token = JwtConfig.generateToken(user)
                call.respond(mapOf("token" to token))
            }
        }
        //认证后才能访问的接口定义使用authenticate定义
        authenticate {
            get("/me") {
                val user = call.user
                call.respond(user)
            }
        }
    }
}

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)


class UserPrincipal(val user: User) : Principal

val ApplicationCall.user: User
    get() = authentication.principal<UserPrincipal>()!!.user

