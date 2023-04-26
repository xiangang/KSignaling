package com.nxg.plugins

import com.nxg.jwt.JwtConfig
import com.nxg.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

fun Application.configureSecurity() {

    authentication {
        jwt {
            verifier(JwtConfig.verifier)
            realm = JwtConfig.realm
            validate { jwtCredential ->
                val id = jwtCredential.payload.subject
                val user = UserRepository.findById(UUID.fromString(id))
                user?.let { UserPrincipal(it) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}
