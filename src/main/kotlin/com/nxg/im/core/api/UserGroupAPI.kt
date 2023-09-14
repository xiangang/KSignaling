package com.nxg.im.core.api

import com.nxg.im.core.data.entity.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userGroupAPI() {
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