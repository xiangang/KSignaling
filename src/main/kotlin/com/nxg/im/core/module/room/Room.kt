package com.nxg.im.core.module.room

data class Room(val id: String, val name: String, val members: MutableList<String>)