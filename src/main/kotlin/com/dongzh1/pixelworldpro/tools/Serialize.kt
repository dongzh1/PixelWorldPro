package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

object Serialize {
    //序列化存入的数据
    fun serialize(worldData: WorldData): String {
        return "${worldData.worldName},|.|," +
                "${worldData.worldLevel},|.|," +
                "${worldData.members.joinToString(",")},|.|," +
                "${worldData.banPlayers.joinToString(",")},|.|," +
                "${worldData.state},|.|," +
                "${worldData.createTime},|.|," +
                "${worldData.lastTime},|.|," +
                "${worldData.onlinePlayerNumber},|.|," +
                "${worldData.isCreateNether},|.|," +
                "${worldData.isCreateEnd}"

    }
    fun deserialize(value: String?): WorldData? {
        if (value == null) {
            return null
        }

        val list = value.split(",|.|,")
        val members = if (list[2].split(",").size == 1 && list[2].split(",")[0] == "") {
            mutableListOf<UUID>()
        } else {
            list[2].split(",").map { UUID.fromString(it) }
        }
        val banPlayers = if (list[3].split(",").size == 1 && list[3].split(",")[0] == "") {
            mutableListOf<UUID>()
        } else {
            list[3].split(",").map { UUID.fromString(it) }
        }
        return WorldData(
            worldName = list[0],
            worldLevel = list[1],
            members = members,
            banPlayers = banPlayers,
            state = list[4],
            createTime = list[5],
            lastTime = list[6].toLong(),
            onlinePlayerNumber = list[7].toInt(),
            isCreateNether = list[8].toBoolean(),
            isCreateEnd = list[9].toBoolean()
        )
    }

    fun serializePlayerData(playerData: PlayerData): String {
        return "${playerData.joinedWorld.joinToString(",")},|.|," +
                "${playerData.memberNumber}"
    }
    fun deserializePlayerData(value: String?): PlayerData? {
        if (value == null) {
            return null
        }

        val list = value.split(",|.|,")
        val joinedWorld = if (list[0].split(",").size == 1 && list[0].split(",")[0] == "") {
            mutableListOf<UUID>()
        } else {
            list[0].split(",").map { UUID.fromString(it) }
        }
        return PlayerData(
            joinedWorld = joinedWorld,
            memberNumber = list[1].toInt()
        )
    }
    fun deserializeLocation(location: String): Location {
        val split = location.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val world = Bukkit.getWorld(split[3])
        return Location(world, split[0].toDouble(), split[1].toDouble(), split[2].toDouble())
    }

    fun serializeLocation(location: Location): String {
        return location.x.toString() + "," + location.y + "," + location.z + "," + location.world!!.name
    }
}