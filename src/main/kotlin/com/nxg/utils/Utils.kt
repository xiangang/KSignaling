package com.nxg.utils

import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.*

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

    fun verifyPassword(password: String, salt: String,hash: String,): Boolean {
        return BCrypt.checkpw(password + salt, hash)
    }
}