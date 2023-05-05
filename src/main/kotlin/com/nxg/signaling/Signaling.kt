package com.nxg.signaling

import kotlinx.serialization.Serializable

@Serializable
data class Signaling(val action: String, val toUsers: List<String>, val payload: String)