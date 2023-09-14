package com.nxg.im.core.plugins

import com.nxg.im.core.api.*
import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.repository.UserRepository
import com.nxg.im.core.data.entity.*
import com.nxg.im.core.utils.SnowflakeUtils
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("KSignaling")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    routing {
        //swaggerUI(path = "openapi")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        authAPI()

        //认证后才能访问的接口使用authenticate定义
        authenticate {

            uidGeneratorAPI()

            userAPI()

            userGroupAPI()

            friendAPI()

            offlineMsgAPI()

        }
    }
}


/**
 * 封装处理Unauthorized的respond
 */
suspend fun PipelineContext<*, ApplicationCall>.respondUnauthorized() {
    call.respond(
        HttpStatusCode.Unauthorized,
        mapOf(
            "code" to HttpStatusCode.Unauthorized.value,
            "message" to "Token is invalid or has expired",
            "data" to null
        )
    )
}


/**
 * 封装处理Authorization获取对应的user
 */
fun PipelineContext<*, ApplicationCall>.getUserByAuthorization(): User? {
    val authorization = call.request.headers["Authorization"]
    val authorizationArray = authorization?.split(" ")
    if (authorizationArray == null || authorizationArray.size < 2) {
        return null
    }
    return JwtConfig.getUserByToken(authorizationArray[1])
}

fun WebSocketServerSession.getUserByAuthorization(): User? {
    val authorization = call.request.headers["Authorization"]
    LOGGER.debug("Authorization: $authorization")
    return authorization?.let { JwtConfig.getUserByToken(it) }
}

