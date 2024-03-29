package com.nxg

import com.google.gson.Gson
import com.nxg.im.core.data.bean.TextMessage
import com.nxg.im.core.data.bean.TextMsgContent
import com.nxg.im.core.data.bean.parseChatMessage
import com.nxg.im.core.data.bean.toJson
import com.nxg.im.core.plugins.LOGGER
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.nxg.im.core.module.sip.SipMessage
import com.nxg.im.core.module.sip.SipMethod
import com.nxg.im.core.module.sip.SipStatus
import com.nxg.im.core.plugins.configureRouting

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
        LOGGER.info("textMessage: ${textMessage.parseChatMessage()}")
        LOGGER.info("textMessage: ${textMessage.parseChatMessage().toJson()}")
    }

    @Test
    fun testSignaling() = testApplication {
        LOGGER.info("testSignaling")
        LOGGER.info("")
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
