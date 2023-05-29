package com.nxg

import com.google.gson.Gson
import com.nxg.im.data.bean.parseIMMessage
import com.nxg.im.data.bean.toJson
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.nxg.im.core.signaling.ADD_FRIEND_INVITE
import com.nxg.im.core.signaling.Signaling
import com.nxg.im.core.signaling.SignalingUser
import com.nxg.im.core.sip.SipMessage
import com.nxg.im.core.sip.SipMethod
import com.nxg.im.core.sip.SipStatus
import com.nxg.im.plugins.configureRouting

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testIMMessage() = testApplication {
        println("testIMMessage")
        /*println(
            TextMessage(
                "51691563050860544",
                "51691563610275840",
                "TextMessage",
                TextMsgContent("123"),
                Timestamp(System.currentTimeMillis()).toString()
            ).toJson()
        )*/
        val imMessage =
            "{\"type\":\"com.nxg.im.TextMessage\",\"sender_id\":\"51691563050860544\",\"receiver_id\":\"51691563610275840\",\"msg_type\":\"TextMessage\",\"msg_content\":{\"text\":\"123\"},\"timestamp\":\"2023-05-28 23:25:17.288\"} "
        println("testIMMessage: ${imMessage.parseIMMessage()}")
        println("testIMMessage: ${imMessage.parseIMMessage().toJson()}")
    }

    @Test
    fun testSignaling() = testApplication {
        println("testSignaling")
        println("")
        val toUser = listOf(SignalingUser(2, 51691563610275840, "xiangang", "福田大飞机"))
        println(Gson().toJson(Signaling(ADD_FRIEND_INVITE, toUser, true, "", 1000, System.currentTimeMillis(), "")))
    }

    @Test
    fun testSipMessage() = testApplication {
        println("testSipMessage")
        println("")
        println(
            SipMessage.createRequest(
                SipMethod.INVITE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            )
        )
        println("")
        println(
            SipMessage.createRequest(
                SipMethod.ACK,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            )
        )
        println("")
        println(
            SipMessage.createRequest(
                SipMethod.CANCEL,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            )
        )
        println("")
        println(
            SipMessage.createRequest(
                SipMethod.BYE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            )
        )
        println("")
        println(
            SipMessage.createRequest(
                SipMethod.BYE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            )
        )
        println("")
        println(
            SipMessage.createResponse(
                SipMessage.createRequest(
                    SipMethod.BYE,
                    "sip:1@192.168.1.5",
                    "sip:1@192.168.1.5",
                    "sip:2@192.168.1.5"
                ), SipStatus.OK
            )
        )
        println("")
    }
}
