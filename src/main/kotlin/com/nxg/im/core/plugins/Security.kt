package com.nxg.im.core.plugins

import com.nxg.im.core.api.UserPrincipal
import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {

    authentication {
        jwt {
            verifier(JwtConfig.verifier)
            realm = JwtConfig.realm
            validate { jwtCredential ->
                val uuid = jwtCredential.payload.subject
                //todo 使用redis
                val user = UserRepository.findByUUId(uuid.toLong())
                user?.let { UserPrincipal(it) }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "code" to HttpStatusCode.Unauthorized.value,
                        "message" to "Token is invalid or has expired",
                        "data" to null
                    )
                )
            }
        }
    }
}
