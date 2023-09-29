package com.nxg.im.core.api

import com.nxg.im.core.data.db.KSignalingDatabase
import com.nxg.im.core.data.entity.FriendTable
import com.nxg.im.core.data.entity.UserTable
import com.nxg.im.core.plugins.getUserByAuthorization
import com.nxg.im.core.plugins.respondUnauthorized
import com.nxg.im.core.repository.FriendRepository
import com.nxg.im.core.repository.UserRepository
import com.nxg.im.core.module.signaling.SignalingManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.jodatime.CurrentDateTime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


fun Route.friendAPI() {
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