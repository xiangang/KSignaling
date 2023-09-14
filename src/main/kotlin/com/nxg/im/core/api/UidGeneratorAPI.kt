package com.nxg.im.core.api

import com.nxg.im.core.utils.SnowflakeUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.uidGeneratorAPI() {
    get("$API_V1/generateUid") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to SnowflakeUtils.snowflake.nextId()
            )
        )
    }
}