package com.nxg.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.nxg.data.entity.User
import com.nxg.utils.HoconUtils
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import java.util.*

object JwtConfig {
    private val config = HoconUtils.config
    private val secret = config.property("ktor.jwt.secret").getString()
    private val issuer = config.property("ktor.jwt.issuer").getString()
    private val audience = config.property("ktor.jwt.audience").getString()
    val realm = config.property("ktor.jwt.realm").getString()
    private const val validityInMs = 3600 * 1000 * 10 // 10 hours
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateToken(user: User): String = JWT.create()
        .withSubject(user.id.toString())
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("username", user.username)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}