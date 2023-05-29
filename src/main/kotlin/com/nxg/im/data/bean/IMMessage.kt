package com.nxg.im.data.bean

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
sealed class IMMessage {
    abstract val sender_id: String
    abstract val receiver_id: String
    abstract val msg_type: String
    abstract val msg_content: MessageContent
    abstract val timestamp: String
}

@Serializable
data class TextMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String,
    override val msg_content: TextMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class ImageMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String,
    override val msg_content: ImageMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class AudioMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String = AudioMessage::class.java.simpleName,
    override val msg_content: AudioMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class VideoMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String,
    override val msg_content: VideoMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class FileMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String,
    override val msg_content: FileMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class LocationMessage(
    override val sender_id: String,
    override val receiver_id: String,
    override val msg_type: String,
    override val msg_content: LocationMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
sealed class MessageContent

@Serializable
data class TextMsgContent(val text: String) : MessageContent()

@Serializable
data class ImageMsgContent(val url: String, val width: Int, val height: Int) : MessageContent()

@Serializable
data class AudioMsgContent(val url: String, val duration: Int) : MessageContent()

@Serializable
data class VideoMsgContent(val url: String, val duration: Int, val width: Int, val height: Int) : MessageContent()

@Serializable
data class FileMsgContent(val url: String, val name: String, val size: Int) : MessageContent()

@Serializable
data class LocationMsgContent(val latitude: Double, val longitude: Double, val address: String) : MessageContent()

fun IMMessage.toJson(): String = Json.encodeToString(IMMessage.serializer(), this)

fun String.parseIMMessage(): IMMessage = Json.decodeFromString(IMMessage.serializer(), this)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = IMMessage::class)
object IMMessageSerializer : KSerializer<IMMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IMMessage") {
        element<String>("sender_id")
        element<String>("receiver_id")
        element<String>("msg_type")
        element<MessageContent>("msg_content")
        element<String>("timestamp")
    }

    override fun serialize(encoder: Encoder, value: IMMessage) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.sender_id)
        composite.encodeStringElement(descriptor, 1, value.receiver_id)
        composite.encodeStringElement(descriptor, 2, value::class.simpleName!!)
        composite.encodeSerializableElement(descriptor, 3, MessageContent.serializer(), value.msg_content)
        composite.encodeStringElement(descriptor, 4, value.timestamp)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): IMMessage {
        val composite = decoder.beginStructure(descriptor)
        lateinit var sender_id: String
        lateinit var receiver_id: String
        lateinit var msg_type: String
        var msg_content: MessageContent? = null
        lateinit var timestamp: String

        loop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> sender_id = composite.decodeStringElement(descriptor, index)
                1 -> receiver_id = composite.decodeStringElement(descriptor, index)
                2 -> msg_type = composite.decodeStringElement(descriptor, index)
                3 -> msg_content = composite.decodeSerializableElement(descriptor, index, MessageContent.serializer())
                4 -> timestamp = composite.decodeStringElement(descriptor, index)
                else -> throw SerializationException("Unknown index $index")
            }
        }

        composite.endStructure(descriptor)
        println("msg_type $msg_type")
        return when (msg_type) {
            TextMessage::class.simpleName -> TextMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? TextMsgContent), timestamp
            )

            ImageMessage::class.simpleName -> ImageMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? ImageMsgContent), timestamp
            )

            AudioMessage::class.simpleName -> AudioMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? AudioMsgContent), timestamp
            )

            VideoMessage::class.simpleName -> VideoMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? VideoMsgContent), timestamp
            )

            FileMessage::class.simpleName -> FileMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? FileMsgContent), timestamp
            )

            LocationMessage::class.simpleName -> LocationMessage(
                sender_id, receiver_id, msg_type,
                checkNotNull(msg_content as? LocationMsgContent), timestamp
            )

            else -> throw SerializationException("Unknown message type $msg_type")
        }
    }
}