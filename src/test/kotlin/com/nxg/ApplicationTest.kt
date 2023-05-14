package com.nxg

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.nxg.plugins.*
import com.nxg.sip.SipMessage
import com.nxg.sip.SipMethod
import com.nxg.sip.SipStatus

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
