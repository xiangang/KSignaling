package com.nxg

import com.google.gson.Gson
import com.nxg.im.core.data.bean.TextMessage
import com.nxg.im.core.data.bean.TextMsgContent
import com.nxg.im.core.data.bean.parseIMMessage
import com.nxg.im.core.data.bean.toJson
import com.nxg.im.core.plugins.LOGGER
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.nxg.im.core.sip.SipMessage
import com.nxg.im.core.sip.SipMethod
import com.nxg.im.core.sip.SipStatus
import com.nxg.im.core.plugins.configureRouting
import com.nxg.im.core.signaling.*

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
        LOGGER.info("testIMMessage")
        val textMessage = TextMessage(
            51691563050860544,
            51691563610275840,
            0,
            TextMsgContent("123"),
            System.currentTimeMillis()
        ).toJson()
        LOGGER.info("textMessage: $textMessage")
        LOGGER.info("textMessage: ${textMessage.parseIMMessage()}")
        LOGGER.info("textMessage: ${textMessage.parseIMMessage().toJson()}")
    }

    @Test
    fun testSignaling() = testApplication {
        LOGGER.info("testSignaling")
        LOGGER.info("")
        val caller = listOf(SignalingUser(53069998221033472, "xiangang", "贤钢"))
        val callee = listOf(SignalingUser(53069999762046976, "yanyan", "燕燕"))
        LOGGER.info(Gson().toJson(Signaling(VIDEO_CALL_INVITE, callee, true, "", 1000, System.currentTimeMillis(), "")))
        LOGGER.info(Gson().toJson(Signaling(VIDEO_CALL_CANCEL, caller, true, "", 1000, System.currentTimeMillis(), "")))
        LOGGER.info(Gson().toJson(Signaling(VIDEO_CALL_ANSWER, callee, true, "", 1000, System.currentTimeMillis(), "")))
        LOGGER.info(Gson().toJson(Signaling(VIDEO_CALL_HANGUP, caller, true, "", 1000, System.currentTimeMillis(), "")))
    }

    @Test
    fun testSipMessage() = testApplication {
        LOGGER.info("testSipMessage")
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createRequest(
                SipMethod.INVITE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            ).toString()
        )
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createRequest(
                SipMethod.ACK,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            ).toString()
        )
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createRequest(
                SipMethod.CANCEL,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            ).toString()
        )
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createRequest(
                SipMethod.BYE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            ).toString()
        )
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createRequest(
                SipMethod.BYE,
                "sip:1@192.168.1.5",
                "sip:1@192.168.1.5",
                "sip:2@192.168.1.5"
            ).toString()
        )
        LOGGER.info("")
        LOGGER.info(
            SipMessage.createResponse(
                SipMessage.createRequest(
                    SipMethod.BYE,
                    "sip:1@192.168.1.5",
                    "sip:1@192.168.1.5",
                    "sip:2@192.168.1.5"
                ), SipStatus.OK
            ).toString()
        )
        LOGGER.info("")
    }
}
