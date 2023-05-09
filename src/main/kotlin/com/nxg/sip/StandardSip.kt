package com.nxg.sip


import io.ktor.websocket.*

/**
 * 标准SIP的信令设计遵循以下原则：

 * 1. SIP信令采用文本格式，使用ASCII码表示，易于阅读和调试。
 * 2. SIP信令使用HTTP风格的请求和响应，包括请求方法、URI、协议版本、消息头和消息体等元素。
 * 3. SIP信令采用分层结构，包括用户层、事务层和传输层。用户层负责处理应用层的请求和响应，事务层负责处理SIP事务的状态和转换，传输层负责处理SIP消息的传输和重传。
 * 4. SIP信令支持多种请求方法，包括INVITE、ACK、BYE、CANCEL、REGISTER、OPTIONS、INFO、PRACK、SUBSCRIBE、NOTIFY、REFER等，每个方法具有特定的语义和用途。
 * 5. SIP信令采用状态机模型，定义了SIP事务的状态转换和超时处理机制，保证了SIP信令的可靠性和稳定性。
 * 6. SIP信令支持请求和响应的路由和转发，包括通过代理服务器、重定向和Location服务等方式实现。
 * 7. SIP信令支持会话描述协议SDP，用于描述媒体流的类型、格式、编码和传输参数等信息。
 * 总之，标准SIP的信令设计具有灵活、可扩展、可靠、可路由和可互操作性等优点，适用于各种IP通信场景。
 *
 * 以下是使用Ktor Websocket仿造标准SIP的信令设计一个通话系统：
 *
 * `CallSession`类代表一个通话会话，
 * `CallState`枚举代表通话状态，
 * `SipMessage`类代表SIP信令消息，
 * `SipMethod`枚举代表SIP请求方法，
 * `SipStatus`类代表SIP响应状态码，
 * `SipHeader`类代表SIP消息头字段。、
 * 在`routing`函数中定义了一个`/sip`的Websocket路由。
 * 当客户端连接到该路由时，创建一个`CallSession`对象表示一个新的通话会话，并将其添加到`callSessions`中。
 * 在`CallSession`类中，`handleMessage`函数用于处理收到的SIP信令消息。
 * 根据SIP请求方法的不同，调用相应的处理函数，如`handleInvite`、`handleAck`和`handleBye`。
 * 处理函数根据当前通话状态和请求内容，生成相应的SIP响应消息，并将其发送给客户端。
 *
 * 在`SipMessage`类中，
 * `createRequest`函数用于创建SIP请求消息，
 * `createResponse`函数用于创建SIP响应消息，
 * `parse`函数用于解析SIP消息文本，
 * `toString`函数用于将SIP消息对象转换为文本。
 *
 * 在`SipMethod`枚举中，`fromValue`函数用于将SIP请求方法的字符串表示转换为枚举值。
 * 在`SipStatus`类中，定义了多个常用的SIP响应状态码。
 * 在`SipHeader`类中，定义了多个常用的SIP消息头字段。
 */
data class CallSession(val callId: String, val webSocketSession: WebSocketSession) {
    private var state: CallState = CallState.Idle

    suspend fun handleMessage(message: String) {
        val request = SipMessage.parse(message)
        val response = when (request.method) {
            SipMethod.INVITE -> handleInvite(request)
            SipMethod.ACK -> handleAck(request)
            SipMethod.BYE -> handleBye(request)
            else -> SipMessage.createResponse(request, SipStatus.BAD_REQUEST, SipMethod.BYE)
        }

        webSocketSession.send(response.toString())
    }

    private fun handleInvite(request: SipMessage): SipMessage {
        if (state != CallState.Idle) {
            return SipMessage.createResponse(request, SipStatus.BUSY_HERE, SipMethod.BYE)
        }

        state = CallState.Ringing
        val response = SipMessage.createResponse(request, SipStatus.RINGING, SipMethod.ACK)
        response.headers[SipHeader.CONTACT] = "<$webSocketSession>"
        return response
    }

    private fun handleAck(request: SipMessage): SipMessage {
        if (state != CallState.Ringing) {
            return SipMessage.createResponse(request, SipStatus.CALL_OR_TRANSACTION_DOES_NOT_EXIST, SipMethod.BYE)
        }

        state = CallState.Active
        return SipMessage.createResponse(request, SipStatus.OK, SipMethod.ACK)
    }

    private fun handleBye(request: SipMessage): SipMessage {
        if (state != CallState.Active) {
            return SipMessage.createResponse(request, SipStatus.CALL_OR_TRANSACTION_DOES_NOT_EXIST, SipMethod.BYE)
        }

        state = CallState.Idle
        return SipMessage.createResponse(request, SipStatus.OK, SipMethod.BYE)
    }

    suspend fun close() {
        if (state != CallState.Idle) {
            val request = SipMessage.createRequest(SipMethod.BYE, callId)
            val response = handleBye(request)
            webSocketSession.send(response.toString())
        }
        webSocketSession.close()
    }
}

enum class CallState {
    Idle,
    Ringing,
    Active
}

fun callSessionId(): String {
    return "call-${System.currentTimeMillis()}"
}

class SipMessage(
    val method: SipMethod,
    val uri: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val body: String? = null
) {
    companion object {
        fun createRequest(method: SipMethod, uri: String): SipMessage {
            return SipMessage(
                method,
                uri,
                mutableMapOf(
                    SipHeader.FROM to "sip:alice@example.com",
                    SipHeader.TO to "sip:bob@example.com",
                    SipHeader.CALL_ID to callSessionId(),
                    SipHeader.CSEQ to "1 ${method.name}"
                ),
                null
            )
        }

        fun createResponse(request: SipMessage, status: SipStatus, sipMethod: SipMethod): SipMessage {
            return SipMessage(
                sipMethod,
                request.uri,
                mutableMapOf(
                    SipHeader.FROM to request.headers[SipHeader.TO]!!,
                    SipHeader.TO to request.headers[SipHeader.FROM]!!,
                    SipHeader.CALL_ID to request.headers[SipHeader.CALL_ID]!!,
                    SipHeader.CSEQ to "${request.headers[SipHeader.CSEQ]!!.split(" ")[0]} ${status.code} ${status.reasonPhrase}"
                ),
                null
            )
        }

        fun parse(text: String): SipMessage {
            val lines = text.lines().map { it.trim() }
            val method = SipMethod.fromValue(lines[0].substringBefore(" ").toUpperCase())
            val uri = lines[0].substringAfter(" ")
            val headers = mutableMapOf<String, String>()
            var body: String? = null

            for (line in lines.drop(1)) {
                if (line.isBlank()) {
                    body = lines.dropWhile { it.isNotBlank() }.joinToString("\n")
                    break
                }

                val key = line.substringBefore(":").toUpperCase()
                val value = line.substringAfter(":").trim()

                if (key != SipHeader.CONTENT_LENGTH) {
                    headers[key] = value
                }
            }

            return SipMessage(method, uri, headers, body)
        }
    }

    override fun toString(): String {
        val lines = mutableListOf<String>()
        lines.add("$method $uri SIP/2.0")

        for ((key, value) in headers) {
            lines.add("$key: $value")
        }

        if (body != null) {
            lines.add("${SipHeader.CONTENT_LENGTH}: ${body.length}")
            lines.add("")
            lines.add(body)
        } else {
            lines.add("")
        }

        return lines.joinToString("\r\n")
    }
}

/**
 * 标准SIP信令包括以下内容：
 *
 * 1. INVITE：邀请对方建立会话。
 * 2. ACK：确认对方已收到INVITE请求。
 * 3. BYE：结束会话。
 * 4. CANCEL：取消正在进行的呼叫。
 * 5. REGISTER：向SIP服务器注册用户信息。
 * 6. OPTIONS：查询SIP服务器支持的功能。
 */
enum class SipMethod(val value: String) {
    INVITE("INVITE"),
    ACK("ACK"),
    BYE("BYE"),
    CANCEL("CANCEL"),
    REGISTER("REGISTER"),
    OPTIONS("OPTIONS");

    companion object {
        fun fromValue(value: String): SipMethod {
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid SIP method: $value")
        }
    }
}

class SipStatus(val code: Int, val reasonPhrase: String) {
    companion object {
        val OK = SipStatus(200, "OK")
        val RINGING = SipStatus(180, "Ringing")
        val BUSY_HERE = SipStatus(486, "Busy Here")
        val CALL_OR_TRANSACTION_DOES_NOT_EXIST = SipStatus(481, "Call/Transaction Does Not Exist")
        val BAD_REQUEST = SipStatus(400, "Bad Request")
    }
}

class SipHeader {
    companion object {
        const val FROM = "FROM"
        const val TO = "TO"
        const val CALL_ID = "CALL-ID"
        const val CSEQ = "CSEQ"
        const val CONTACT = "CONTACT"
        const val CONTENT_LENGTH = "CONTENT-LENGTH"
    }
}
