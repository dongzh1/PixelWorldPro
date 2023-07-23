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
                "${worldData.members.joinToString(",")},,|.|," +
                "${worldData.memberName.joinToString(",")},,|.|," +
                "${worldData.banPlayers.joinToString(",")},,|.|," +
                "${worldData.banName.joinToString(",")},,|.|," +
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
        val members = if ((list[2].split(",").size == 1 && list[2].split(",")[0] == "")||
            (list[2].split(",").size == 2 && list[2].split(",")[1] == ""&&list[2].split(",")[0] == "")) {
            mutableListOf<UUID>()
        } else {
            list[2].split(",").dropLast(1).map{ UUID.fromString(it) }
        }
        val banPlayers = if ((list[4].split(",").size == 1 && list[4].split(",")[0] == "")||
            (list[4].split(",").size == 2 && list[4].split(",")[1] == ""&&list[4].split(",")[0] == "")) {
            mutableListOf<UUID>()
        } else {
            list[4].split(",").dropLast(1).map { UUID.fromString(it) }
        }
        val memberName = if ((list[3].split(",").size == 1 && list[3].split(",")[0] == "")||
            (list[3].split(",").size == 2 && list[3].split(",")[1] == ""&&list[3].split(",")[0] == "")) {
            mutableListOf<String>()
        } else {
            list[3].split(",").dropLast(1)
        }
        val banName = if ((list[5].split(",").size == 1 && list[5].split(",")[0] == "")||
            (list[5].split(",").size == 2 && list[5].split(",")[1] == ""&&list[5].split(",")[0] == "")) {
            mutableListOf<String>()
        } else {
            list[5].split(",").dropLast(1)
        }
        return WorldData(
            worldName = list[0],
            worldLevel = list[1],
            members = members,
            memberName = memberName,
            banPlayers = banPlayers,
            banName = banName,
            state = list[6],
            createTime = list[7],
            lastTime = list[8].toLong(),
            onlinePlayerNumber = list[9].toInt(),
            isCreateNether = list[10].toBoolean(),
            isCreateEnd = list[11].toBoolean()
        )
    }

    fun serializePlayerData(playerData: PlayerData): String {
        return "${playerData.joinedWorld.joinToString(",")},,|.|," +
                "${playerData.memberNumber}"
    }
    fun deserializePlayerData(value: String?): PlayerData? {
        if (value == null) {
            return null
        }

        val list = value.split(",|.|,")
        val joinedWorld = if ((list[0].split(",").size == 1 && list[0].split(",")[0] == "")||
            (list[0].split(",").size == 2 && list[0].split(",")[1] == ""&&list[0].split(",")[0] == "")) {
            mutableListOf<UUID>()
        } else {
            //去掉最后一个
            list[0].split(",").dropLast(1).map { UUID.fromString(it) }
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