package com.nxg.data.entity

import java.util.*

data class User(
    val id: UUID,
    val username: String,
    val passwordHash: String
)