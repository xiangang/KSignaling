package com.nxg.im.data.bean

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.properties.Delegates

@Serializable
sealed class IMMessage {
    abstract val from_id: Long
    abstract val to_id: Long
    abstract val chat_type: Int
    abstract val content: MessageContent
    abstract val timestamp: String
}

@Serializable
data class TextMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: TextMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class ImageMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: ImageMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class AudioMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: AudioMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class VideoMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: VideoMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class FileMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: FileMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
data class LocationMessage(
    override val from_id: Long,
    override val to_id: Long,
    override val chat_type: Int,
    override val content: LocationMsgContent,
    override val timestamp: String
) : IMMessage()

@Serializable
sealed class MessageContent

fun MessageContent.toJson(): String = Json.encodeToString(MessageContent.serializer(), this)

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

/*
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = IMMessage::class)
object IMMessageSerializer : KSerializer<IMMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IMMessage") {
        element<Long>("from_id")
        element<Long>("to_id")
        element<Int>("chat_type")
        element<String>("msg_type")
        element<MessageContent>("content")
        element<String>("timestamp")
    }

    override fun serialize(encoder: Encoder, value: IMMessage) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeLongElement(descriptor, 0, value.from_id)
        composite.encodeLongElement(descriptor, 1, value.to_id)
        composite.encodeIntElement(descriptor, 2, value.chat_type)
        composite.encodeStringElement(descriptor, 3, value::class.simpleName!!)
        composite.encodeSerializableElement(descriptor, 4, MessageContent.serializer(), value.content)
        composite.encodeStringElement(descriptor, 5, value.timestamp)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): IMMessage {
        val composite = decoder.beginStructure(descriptor)
        var from_id by Delegates.notNull<Long>()
        var to_id by Delegates.notNull<Long>()
        var chat_type by Delegates.notNull<Int>()
        lateinit var msg_type: String
        lateinit var content: MessageContent
        lateinit var timestamp: String

        loop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> from_id = composite.decodeLongElement(descriptor, index)
                1 -> to_id = composite.decodeLongElement(descriptor, index)
                2 -> chat_type = composite.decodeIntElement(descriptor, index)
                3 -> msg_type = composite.decodeStringElement(descriptor, index)
                4 -> content = composite.decodeSerializableElement(descriptor, index, MessageContent.serializer())
                5 -> timestamp = composite.decodeStringElement(descriptor, index)
                else -> throw SerializationException("Unknown index $index")
            }
        }

        composite.endStructure(descriptor)
        println("deserialize msg_type $msg_type")
        return when (msg_type) {
            TextMessage::class.simpleName -> TextMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? TextMsgContent), timestamp
            )

            ImageMessage::class.simpleName -> ImageMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? ImageMsgContent), timestamp
            )

            AudioMessage::class.simpleName -> AudioMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? AudioMsgContent), timestamp
            )

            VideoMessage::class.simpleName -> VideoMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? VideoMsgContent), timestamp
            )

            FileMessage::class.simpleName -> FileMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? FileMsgContent), timestamp
            )

            LocationMessage::class.simpleName -> LocationMessage(
                from_id, to_id, chat_type, msg_type,
                checkNotNull(content as? LocationMsgContent), timestamp
            )

            else -> throw SerializationException("Unknown message type $msg_type")
        }
    }
}*/
