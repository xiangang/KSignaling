package com.nxg.im.core.repository

import com.nxg.im.core.data.entity.Friend
import com.nxg.im.core.data.entity.FriendTable
import com.nxg.im.core.data.entity.toSimpleUser
import org.jetbrains.exposed.sql.ResultRow

object FriendRepository {
    fun toFriend(row: ResultRow): Friend {
        val user = UserRepository.toUser(row).toSimpleUser()
        return Friend(
            uuid = user.uuid,
            username = user.username,
            email = user.email,
            phone = user.phone,
            nickname = user.nickname,
            avatar = user.avatar,
            gender = user.gender,
            birthday = user.birthday,
            country = user.country,
            province = user.province,
            address = user.address,
            status = user.status,
            relationType = row[FriendTable.relation_type],
            relationStatus = row[FriendTable.relation_status],
            remark = row[FriendTable.remark],
            permission = row[FriendTable.permission],
            groupId = row[FriendTable.group_id],
            latestDynamic = row[FriendTable.latest_dynamic].toString(),
            isRecommend = row[FriendTable.is_recommend],
            createTime = row[FriendTable.create_time].millis,
            updateTime =  row[FriendTable.update_time].millis
        )
    }
}