package com.nxg.im.core.module.signaling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class SignalingType {
    @Serializable
    @SerialName("VideoCall")
    object VideoCall : SignalingType()

    @Serializable
    @SerialName("AudioCall")
    object AudioCall : SignalingType()
}

@Serializable
@SerialName("Signaling")
data class Signaling(
    val id: Long, //唯一id
    val fromId: Long, //用户id
    val signalingType: SignalingType, //信令类型：video_call，audio_call
    val cmd: String, //信令命令：invite，cancel，answer，bye，
    val participants: List<Long>,
    val onlineUserOnly: Boolean,
    val offlinePushInfo: String,
    val timeout: Int,
    val createTime: Long,
    val payload: String
)

fun Signaling.toJson(): String = Json.encodeToString(Signaling.serializer(), this)

fun String.parseSignaling(): Signaling = Json.decodeFromString(Signaling.serializer(), this)

object SignalingHelper {

    fun createVideoCallInvite(
        id: Long,
        fromId: Long,
        toUsers: List<Long>,
        payload: String = ""
    ): Signaling {
        return Signaling(
            id,
            fromId,
            SignalingType.VideoCall,
            "invite",
            toUsers,
            true,
            "",
            10,
            System.currentTimeMillis(),
            payload
        )
    }

    fun createVideoCallAnswer(
        id: Long,
        fromId: Long,
        toUsers: List<Long>,
        payload: String = ""
    ): Signaling {
        return Signaling(
            id,
            fromId,
            SignalingType.VideoCall,
            "answer",
            toUsers,
            true,
            "",
            10,
            System.currentTimeMillis(),
            payload
        )
    }

    fun createVideoCallCancel(
        id: Long,
        fromId: Long,
        toUsers: List<Long>,
        payload: String = ""
    ): Signaling {
        return Signaling(
            id,
            fromId,
            SignalingType.VideoCall,
            "cancel",
            toUsers,
            true,
            "",
            10,
            System.currentTimeMillis(),
            payload
        )
    }

    fun createVideoCallBye(
        id: Long,
        fromId: Long,
        toUsers: List<Long>,
        payload: String = ""
    ): Signaling {
        return Signaling(
            id,
            fromId,
            SignalingType.VideoCall,
            "bye",
            toUsers,
            true,
            "",
            10,
            System.currentTimeMillis(),
            payload
        )
    }

}