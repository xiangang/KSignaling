package com.nxg.im.core.plugins

import com.nxg.im.core.IMCoreMessage
import com.nxg.im.core.data.db.KSignalingDatabase
import com.nxg.im.core.jwt.JwtConfig
import com.nxg.im.core.repository.UserRepository
import com.nxg.im.core.signaling.SignalingManager
import com.nxg.im.core.data.bean.OfflineMsg
import com.nxg.im.core.data.entity.*
import com.nxg.im.core.data.redis.KSignalingRedisClient
import com.nxg.im.core.repository.FriendRepository
import com.nxg.im.core.utils.PasswordUtils
import com.nxg.im.core.utils.PasswordUtils.hashPassword
import com.nxg.im.core.utils.PasswordUtils.verifyPassword
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
import io.ktor.util.pipeline.*
import io.lettuce.core.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDateTime
import java.nio.charset.StandardCharsets
import kotlin.text.toByteArray

const val API_V1 = "/api/v1"
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

            handleUidGenerator()

            handleUser()

            handleGroup()

            handleFriend()

            handleOfflineMsg()

        }
    }
}


/**
 * 封装处理Unauthorized的respond
 */
private suspend fun PipelineContext<*, ApplicationCall>.respondUnauthorized() {
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
private fun PipelineContext<*, ApplicationCall>.getUserByAuthorization(): User? {
    val authorization = call.request.headers["Authorization"]
    val authorizationArray = authorization?.split(" ")
    if (authorizationArray == null || authorizationArray.size < 2) {
        return null
    }
    return JwtConfig.getUserByToken(authorizationArray[1])
}

private fun Route.handleOfflineMsg() {
    //获取离线消息
    get("$API_V1/offlineMsg") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@get
        }
        val pageIndex = call.parameters["pageIndex"]?.toInt() ?: 0
        val limit = call.parameters["pageSize"]?.toInt() ?: 20
        val fromId = call.parameters["fromId"]
        val messageId = call.parameters["messageId"]
        if (fromId.isNullOrEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to HttpStatusCode.BadRequest.value,
                    "message" to "Missing fromId parameter",
                    "data" to null
                )
            )
            return@get
        }
        if (messageId.isNullOrEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to HttpStatusCode.BadRequest.value,
                    "message" to "Missing messageId parameter",
                    "data" to null
                )
            )
            return@get
        }
        // 从Redis获取离线消息
        try {
            val offset = pageIndex * limit
            val min = java.lang.Double.longBitsToDouble(messageId.toLong() + 1)
            val redisCommands = KSignalingRedisClient.redisClientConnection.sync()
            val key = "offline:${user.uuid}-$fromId"
            LOGGER.info("offlineMessages: key $key, min $min, offset $offset, limit $limit")
            val offlineMessages = redisCommands.zrangebyscore(
                key,
                Range.create(min, Long.MAX_VALUE),
                Limit.create(offset.toLong(), limit.toLong())
            )
            val count = redisCommands.zcount(key, Range.create(min, Long.MAX_VALUE).gt(min))
            LOGGER.info("offlineMessages: count $count")
            val messages = mutableListOf<String>()
            offlineMessages.forEach {
                val imCoreMessage = IMCoreMessage.parseFrom(it.toByteArray(StandardCharsets.ISO_8859_1))
                val imMessageJson = imCoreMessage.bodyData.toStringUtf8()
                LOGGER.info("offlineMessages: imMessageJson $imMessageJson")
                messages.add(it)
            }
            LOGGER.info("offlineMessages: messages $messages")
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.OK.value,
                    "message" to "Success",
                    "data" to OfflineMsg(count, messages)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf(
                    "code" to HttpStatusCode.BadGateway.value,
                    "message" to "{${e.message}}",
                    "data" to null
                )
            )
        }
    }
}

private fun Route.handleFriend() {
    get("$API_V1/myFriends") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@get
        }
        val friends = transaction {
            (UserTable innerJoin FriendTable).select { FriendTable.user_id eq user.uuid }
                .map {
                    FriendRepository.toFriend(it)
                }
        }
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to friends
            )
        )
    }
    post("$API_V1/addFriend") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@post
        }
        val friendId = call.receiveParameters()["friendId"]?.toLongOrNull()
        if (friendId == null) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Missing friendId parameter",
                    "data" to null
                )
            )
            return@post
        }
        // 验证用户是否存在
        val friend = UserRepository.findByUUId(friendId)
        if (friend == null) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Friend not exist!",
                    "data" to null
                )
            )
            return@post
        }
        transaction(KSignalingDatabase.database) {
            //好友关系表维护两条数据
            FriendTable.insert {
                it[user_id] = user.uuid
                it[friend_id] = friendId
                it[create_time] = CurrentDateTime
                it[update_time] = CurrentDateTime
            }
            FriendTable.insert {
                it[user_id] = friendId
                it[friend_id] = user.uuid
                it[create_time] = CurrentDateTime
                it[update_time] = CurrentDateTime
            }
        }
        //websocket通知
        SignalingManager.sendMsg2User(user.uuid, "")

        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to null
            )
        )
    }

    post("$API_V1/ackAddFriendRequest") {
        val user = getUserByAuthorization()
        if (user == null) {
            respondUnauthorized()
            return@post
        }
        val parameters = call.receiveParameters()
        val friendId = parameters["friendId"]?.toLongOrNull()
        if (friendId == null) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Missing friendId parameter",
                    "data" to null
                )
            )
            return@post
        }
        val agree = parameters["agree"]?.toIntOrNull()
        if (agree == null) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Missing agree parameter",
                    "data" to null
                )
            )
            return@post
        }
        val friend = UserRepository.findByUUId(friendId)
        if (friend == null) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Friend not exist!",
                    "data" to null
                )
            )
            return@post
        }
        if (user.uuid == friend.uuid) {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "code" to HttpStatusCode.NoContent.value,
                    "message" to "Cannot agree self",
                    "data" to null
                )
            )
            return@post
        }
        transaction(KSignalingDatabase.database) {
            FriendTable.update({ (FriendTable.user_id eq user.uuid) and (FriendTable.friend_id eq friend.uuid) }) {
                it[relation_status] = agree
                it[update_time] = CurrentDateTime
            }
            FriendTable.update({ (FriendTable.user_id eq friend.uuid) and (FriendTable.friend_id eq user.uuid) }) {
                it[relation_status] = agree
                it[update_time] = CurrentDateTime
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
}

private fun Route.handleUidGenerator() {
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

private fun Route.handleUser() {
    get("$API_V1/me") {
        val user = call.user
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "code" to HttpStatusCode.OK.value,
                "message" to HttpStatusCode.OK.description,
                "data" to user.toSimpleUser()
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
}

private fun Route.handleGroup() {
    //获取指定用户所在的所有群组
    get("$API_V1/user/{userId}/groups") {
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
                        it[GroupTable.creatorId]
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
    post("$API_V1/groups/{groupId}/join") {
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
            GroupMemberTable.insertAndGetId {
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
                        it[GroupTable.creatorId]
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
                    it[GroupMemberTable.userId]
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
                    it[GroupMessageTable.senderId],
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

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)


class UserPrincipal(val user: User) : Principal

val ApplicationCall.user: User
    get() = authentication.principal<UserPrincipal>()!!.user

