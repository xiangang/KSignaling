package com.nxg.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.nxg.data.entity.User
import java.util.*

object JwtConfig {
    private const val secret = "ktor.io"
    private const val issuer = "ktor.io"
    private const val audience = "ktor.io"
    private const val validityInMs = 3600 * 1000 * 10 // 10 hours

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(user: User): String = JWT.create()
        .withSubject(user.id.toString())
        .withIssuer(issuer)
        .withAudience(audience)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}