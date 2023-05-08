package com.nxg.plugins

import com.nxg.data.entity.*
import com.nxg.jwt.JwtConfig
import com.nxg.repository.UserRepository
import com.nxg.utils.PasswordUtils
import com.nxg.utils.PasswordUtils.hashPassword
import com.nxg.utils.PasswordUtils.verifyPassword
import com.nxg.utils.SnowflakeUtils
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDateTime
import java.util.*

const val API_V1 = "/api/v1"
fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
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
        post("$API_V1/register") {
            val request = call.receive<RegisterRequest>()
            val user = UserRepository.findByUsername(request.username)
            if (user != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "code" to HttpStatusCode.BadRequest.value,
                        "message" to "Username already exists",
                        "data" to null
                    )
                )
            } else {
                val salt: String = PasswordUtils.generateSalt(16)
                val passwordHash = hashPassword(request.password, salt)
                val now = LocalDateTime.now()
                val newUser = User(
                    0,
                    SnowflakeUtils.snowflake.nextId(),
                    request.username,
                    passwordHash,
                    salt,
                    createTime = now,
                    updateTime = now
                )
                UserRepository.save(newUser)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to newUser.toSimpleUser()
                    )
                )
            }
        }
        post("$API_V1/login") {
            val request = call.receive<LoginRequest>()
            val user = UserRepository.findByUsername(request.username)
            if (user == null || !verifyPassword(request.password, user.salt, user.password)) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "code" to HttpStatusCode.Unauthorized.value,
                        "message" to "Invalid username or password",
                        "data" to null
                    )
                )
            } else {
                val token = JwtConfig.generateToken(user)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to mapOf("token" to token, "user" to user.toSimpleUser())
                    )
                )
            }
        }

        //认证后才能访问的接口使用authenticate定义
        authenticate {
            get("$API_V1/me") {
                val user = call.user
                call.respond(user)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to user
                    )
                )
            }

            // 获取所有用户
            get("$API_V1/users") {
                val users = transaction {
                    UserTable.selectAll().map { UserRepository.toUser(it).toSimpleUser() }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to users
                    )
                )
            }

            get("/user/{userId}/groups") {
                val userId = call.parameters["userId"]?.toLongOrNull()
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "code" to HttpStatusCode.OK.value,
                            "message" to "Invalid user ID",
                            "data" to null
                        )
                    )
                    return@get
                }

                val groups = transaction {
                    (GroupTable innerJoin GroupMemberTable)
                        .select { GroupMemberTable.userId eq userId }
                        .map {
                            Group(
                                it[GroupTable.id].value,
                                it[GroupTable.groupName],
                                it[GroupTable.creatorId].value
                            )
                        }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to groups
                    )
                )
            }

            //加入群组
            post("/groups/{groupId}/join") {
                val joinGroupId = call.parameters["groupId"]?.toLongOrNull()
                if (joinGroupId == null) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "code" to HttpStatusCode.OK.value,
                            "message" to "Missing groupId parameter",
                            "data" to null
                        )
                    )
                    return@post
                }
                val joinGroupRequest = call.receive<JoinGroupRequest>()
                val joinUserId = joinGroupRequest.userId
                transaction {
                    GroupMemberTable.insert {
                        it[groupId] = joinGroupId
                        it[userId] = joinUserId
                    }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to null
                    )
                )
            }

            // 获取所有群组
            get("$API_V1/groups") {
                val groups = transaction {
                    GroupTable.selectAll()
                        .map {
                            Group(
                                it[GroupTable.id].value,
                                it[GroupTable.groupName],
                                it[GroupTable.creatorId].value
                            )
                        }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to groups
                    )
                )
            }

            // 获取所有群组成员
            get("$API_V1/group-members") {
                val groupMembers = transaction {
                    GroupMemberTable.selectAll().map {
                        GroupMember(
                            it[GroupMemberTable.id].value,
                            it[GroupMemberTable.groupId].value,
                            it[GroupMemberTable.userId].value
                        )
                    }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to groupMembers
                    )
                )
            }

            // 获取所有群组消息
            get("$API_V1/group-messages") {
                val groupMessages = transaction {
                    GroupMessageTable.selectAll().map {
                        GroupMessage(
                            it[GroupMessageTable.id].value,
                            it[GroupMessageTable.groupId].value,
                            it[GroupMessageTable.senderId].value,
                            it[GroupMessageTable.messageContent]
                        )
                    }
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to groupMessages
                    )
                )
            }

            // 创建一个群组
            post("$API_V1/group") {
                val group = call.receive<Group>()
                val groupId = transaction {
                    GroupTable.insertAndGetId {
                        it[groupName] = group.groupName
                        it[creatorId] = group.creatorId
                    }.value
                }
                call.respond(
                    HttpStatusCode.Created,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to Group(groupId, group.groupName, group.creatorId)
                    )
                )
            }

            // 增加群组成员
            post("$API_V1/group-members") {
                val groupMember = call.receive<GroupMember>()
                val memberId = transaction {
                    GroupMemberTable.insertAndGetId {
                        it[groupId] = groupMember.groupId
                        it[userId] = groupMember.userId
                    }.value
                }
                call.respond(
                    HttpStatusCode.Created,
                    mapOf(
                        "code" to HttpStatusCode.OK.value,
                        "message" to HttpStatusCode.OK.description,
                        "data" to GroupMember(memberId, groupMember.groupId, groupMember.userId)
                    )
                )
            }

            // 插入群组消息
            post("$API_V1/group-messages") {
                val groupMessage = call.receive<GroupMessage>()
                val messageId = transaction {
                    GroupMessageTable.insertAndGetId {
                        it[groupId] = groupMessage.groupId
                        it[senderId] = groupMessage.senderId
                        it[messageContent] = groupMessage.messageContent
                    }.value
                }
                call.respond(
                    HttpStatusCode.Created,
                    GroupMessage(messageId, groupMessage.groupId, groupMessage.senderId, groupMessage.messageContent)
                )
            }
        }
    }
}

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)


class UserPrincipal(val user: User) : Principal

val ApplicationCall.user: User
    get() = authentication.principal<UserPrincipal>()!!.user

