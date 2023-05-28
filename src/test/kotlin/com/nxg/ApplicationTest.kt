package com.nxg

import com.google.gson.Gson
import com.nxg.im.TextMessage
import com.nxg.im.TextMsgContent
import com.nxg.im.toJson
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.nxg.plugins.*
import com.nxg.signaling.ADD_FRIEND_INVITE
import com.nxg.signaling.Signaling
import com.nxg.signaling.SignalingUser
import com.nxg.sip.SipMessage
import com.nxg.sip.SipMethod
import com.nxg.sip.SipStatus
import com.nxg.utils.GsonUtils
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
        println(
            TextMessage(
                "51691563050860544",
                "51691563610275840",
                "TextMessage",
                TextMsgContent("123"),
                Timestamp(System.currentTimeMillis()).toString()
            ).toJson()
        )
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
