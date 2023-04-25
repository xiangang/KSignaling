package com.nxg.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordUtils {
    private const val salt = "my_salt"

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password + salt, BCrypt.gensalt())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password + salt, hash)
    }
}