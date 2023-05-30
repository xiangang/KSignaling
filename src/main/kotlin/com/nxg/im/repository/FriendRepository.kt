package com.nxg.im.repository

import com.nxg.im.data.entity.*
import org.jetbrains.exposed.sql.ResultRow

object FriendRepository {
    fun toFriend(row: ResultRow): Friend {
        return Friend(
            user = UserRepository.toUser(row).toSimpleUser(),
            relationType = row[FriendTable.relation_type],
            relationStatus = row[FriendTable.relation_status],
            remark = row[FriendTable.remark],
            permission = row[FriendTable.permission],
            groupId = row[FriendTable.group_id],
            latestDynamic = row[FriendTable.latest_dynamic].toString(),
            isRecommend = row[FriendTable.is_recommend],
            createTime = row[FriendTable.create_time].toLocalDateTime().toString(patternYmdHms),
            updateTime = row[FriendTable.update_time].toLocalDateTime().toString(patternYmdHms),
        )
    }
}