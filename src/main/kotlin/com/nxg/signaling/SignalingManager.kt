package com.nxg.signaling

import java.util.*
import kotlin.collections.LinkedHashSet

object SignalingManager {

    val signalingSessions = Collections.synchronizedSet<SignalingSession>(LinkedHashSet())
}