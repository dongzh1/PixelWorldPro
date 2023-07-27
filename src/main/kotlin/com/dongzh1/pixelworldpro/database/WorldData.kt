package com.dongzh1.pixelworldpro.database

import java.util.UUID

data class WorldData (
    val worldName: String,
    val worldLevel: String,
    val members: List<UUID>,
    val memberName: List<String>,
    val banPlayers: List<UUID>,
    val banName: List<String>,
    //anyone, member, owner
    val state: String,
    val createTime: String,
    val lastTime: Long,
    val onlinePlayerNumber: Int,
    val isCreateNether: Boolean,
    val isCreateEnd: Boolean
)
data class PlayerData (
    val joinedWorld: List<UUID>,
    val memberNumber: Int
)