package com.nxg.push

import com.nxg.connection.Connection
import java.util.*
import kotlin.collections.LinkedHashSet

object PushManager {

    val pushSessions = Collections.synchronizedSet<PushSession>(LinkedHashSet())
}