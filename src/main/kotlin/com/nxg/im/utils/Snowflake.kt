package com.nxg.im.utils

class Snowflake(val workerId: Long) {
    private val twepoch = 1288834974657L
    private val workerIdBits = 5
    private val maxWorkerId = -1L xor (-1L shl workerIdBits)
    private val sequenceBits = 12

    private val workerIdShift = sequenceBits
    private val timestampLeftShift = sequenceBits + workerIdBits
    private val sequenceMask = -1L xor (-1L shl sequenceBits)

    private var sequence = 0L
    private var lastTimestamp = -1L

    init {
        require(workerId in 0..maxWorkerId) { "workerId must be between 0 and $maxWorkerId" }
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp = System.currentTimeMillis()

        if (timestamp < lastTimestamp) {
            throw RuntimeException("Clock moved backwards. Refusing to generate id")
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) and sequenceMask
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return (timestamp - twepoch shl timestampLeftShift) or (workerId shl workerIdShift) or sequence
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}