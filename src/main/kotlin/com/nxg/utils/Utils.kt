package com.nxg.utils

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.*

object HoconUtils {
    val config = HoconApplicationConfig(ConfigFactory.load())
}

object PasswordUtils {

    fun generateSalt(length: Int): String {
        val random = SecureRandom()
        val salt = ByteArray(length)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPassword(password: String, salt: String = generateSalt(16)): String {
        return BCrypt.hashpw(password + salt, BCrypt.gensalt())
    }

    fun verifyPassword(password: String, salt: String, hash: String): Boolean {
        return BCrypt.checkpw(password + salt, hash)
    }
}

object SnowflakeUtils {
    private val workId = HoconUtils.config.property("ktor.workId").getString().toLong()
    val snowflake by lazy {
        Snowflake(workId)
    }

}