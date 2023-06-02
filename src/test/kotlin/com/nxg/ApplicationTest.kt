package com.nxg

import com.google.gson.Gson
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
import com.nxg.im.data.bean.*
import com.nxg.im.plugins.configureRouting
import java.sql.Timestamp

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
        val textMessage = TextMessage(
            51691563050860544,
            51691563610275840,
            0,
            TextMsgContent("123"),
            Timestamp(System.currentTimeMillis()).toString()
        ).toJson()
        println("textMessage: $textMessage")
        println("textMessage: ${textMessage.parseIMMessage()}")
        println("textMessage: ${textMessage.parseIMMessage().toJson()}")
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
