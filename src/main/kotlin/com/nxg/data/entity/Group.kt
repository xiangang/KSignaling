package com.nxg.data.entity

data class Group(val id: Long, val groupName: String, val creatorId: Long)
data class GroupMember(val id: Long, val groupId: Long, val userId: Long)
data class GroupMessage(val id: Long, val groupId: Long, val senderId: Long, val messageContent: String)

data class JoinGroupRequest(val userId: Long)