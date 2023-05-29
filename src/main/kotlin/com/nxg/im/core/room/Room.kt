package com.nxg.im.core.room

data class Room(val id: String, val name: String, val members: MutableList<String>)