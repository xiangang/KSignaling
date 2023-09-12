package com.nxg.im.core.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.nxg.im.core.data.entity.User
import com.nxg.im.core.plugins.LOGGER
import com.nxg.im.core.repository.UserRepository
import com.nxg.im.core.utils.HoconUtils
import java.time.Duration
import java.util.*

object JwtConfig {
    private val config = HoconUtils.config
    private val secret = config.property("ktor.jwt.secret").getString()
    private val issuer = config.property("ktor.jwt.issuer").getString()
    private val audience = config.property("ktor.jwt.audience").getString()
    val realm = config.property("ktor.jwt.realm").getString()
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        //.withAudience(audience) //暂时没有必要，尽量缩短token长度
        //.withIssuer(issuer)
        .build()

    fun generateToken(user: User): String = JWT.create()
        .withSubject(user.uuid.toString())
        //.withAudience(audience) //暂时没有必要，尽量缩短token长度
        //.withIssuer(issuer)
        //.withClaim("username", user.username)
        .withExpiresAt(Date(System.currentTimeMillis() + Duration.ofDays(60).toMillis()))//60天过期
        .sign(algorithm)

    fun getUserByToken(token: String): User? {
        try {
             LOGGER.info("isValidToken token $token")
            val jwt = verifier.verify(token)
            if (jwt.expiresAt == null) {
                 LOGGER.info("isValidToken token expired")
                return null
            }
            val uuid = jwt.subject
             LOGGER.info("isValidToken uuid $uuid")
            return UserRepository.findByUUId(uuid.toLong())
        } catch (e: JWTVerificationException) {
             LOGGER.info("isValidToken ${e.message}")
        }
        return null
    }
}