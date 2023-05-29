package com.nxg.im.core.signaling

import kotlinx.serialization.Serializable

typealias SignalingType = String

/**
 * 添加好友
 */
const val ADD_FRIEND_INVITE: SignalingType = "ADD_FRIEND_INVITE"
const val ADD_FRIEND_AGREE: SignalingType = "ADD_FRIEND_AGREE"
const val ADD_FRIEND_REJECT: SignalingType = "ADD_FRIEND_REJECT"
const val ADD_FRIEND_BACK_LIST: SignalingType = "ADD_FRIEND_BACK_LIST"

/**
 * 视频通话信令
 */
const val VIDEO_CALL_INVITE: SignalingType = "VIDEO_CALL_INVITE"
const val VIDEO_CALL_CANCEL: SignalingType = "VIDEO_CALL_CANCEL"
const val VIDEO_CALL_ANSWER: SignalingType = "VIDEO_CALL_ANSWER"
const val VIDEO_CALL_HANGUP: SignalingType = "VIDEO_CALL_HANGUP"

/**
 * 音频通话信令
 */
const val AUDIO_CALL_INVITE: SignalingType = "AUDIO_CALL_INVITE"
const val AUDIO_CALL_CANCEL: SignalingType = "AUDIO_CALL_CANCEL"
const val AUDIO_CALL_ANSWER: SignalingType = "AUDIO_CALL_ANSWER"
const val AUDIO_CALL_HANGUP: SignalingType = "AUDIO_CALL_HANGUP"

/**
 * 群信令
 */
const val GROUP_CHAT_JOIN: SignalingType = "GROUP_CHAT_JOIN"
const val GROUP_CHAT_LEAVE: SignalingType = "GROUP_CHAT_LEAVE"
const val GROUP_CHAT_NOTICE: SignalingType = "GROUP_CHAT_NOTICE"

@Serializable
data class Signaling(
    val signalingType: SignalingType,
    val toUsers: List<SignalingUser>,
    val onlineUserOnly: Boolean,
    val offlinePushInfo: String,
    val timeout: Int,
    val createTime: Long,
    val payload: String
)

@Serializable
sealed class SignalingPayload() {

    /**
     * status:0邀请，1同意，2拒绝，3拉黑
     */
    class SignalingAddFriend(status: Int)

    class SignalingVideoCall(status: Int)
}

@Serializable
data class SignalingUser(val userId: Long, val uuid: Long, val username: String, val nickname: String)