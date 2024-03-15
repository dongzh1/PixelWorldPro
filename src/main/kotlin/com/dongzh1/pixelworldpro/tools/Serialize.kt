package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.world.Level
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.Location
import org.json.simple.JSONObject
import top.shadowpixel.shadowlevels.api.ShadowLevelsAPI
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
                "${worldData.isCreateEnd},|.|," +
                "${worldData.inviter.joinToString(", ")},|.|," +
                JSONObject.toJSONString(worldData.gameRule).toString() + ",|.|," +
                JSONObject.toJSONString(worldData.location).toString()
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
            mutableListOf()
        } else {
            list[3].split(",").dropLast(1)
        }
        val banName = if ((list[5].split(",").size == 1 && list[5].split(",")[0] == "")||
            (list[5].split(",").size == 2 && list[5].split(",")[1] == ""&&list[5].split(",")[0] == "")) {
            mutableListOf()
        } else {
            list[5].split(",").dropLast(1)
        }
        val inviter = mutableListOf<UUID>()
        if (list.size >= 13) {
            if(list[12] != "") {
                for (id in list[12].split(",")) {
                    inviter.add(UUID.fromString(id))
                }
            }
        }
        val gameRule = HashMap<String, String>()
        try {
            if (list.size >= 14) {
                val g = Gson()
                val type = object : TypeToken<Map<String, String>>() {}.type
                val back: JsonObject = g.fromJson(list[13], JsonObject::class.java)
                val gson = GsonBuilder().enableComplexMapKeySerialization().create()
                val map: Map<String, String> = gson.fromJson(back, type)
                for (key in map.keys){
                    gameRule[key] = back.get(key).asString
                }
            }
        } catch (e:Exception){
            println(e)
        }
        val location = HashMap<String, Double>()
        try {
            if (list.size >= 15) {
                val g = Gson()
                val back: JsonObject = g.fromJson(list[14], JsonObject::class.java)
                val type = object : TypeToken<Map<String, String>>() {}.type
                val gson = GsonBuilder().enableComplexMapKeySerialization().create()
                val map: Map<String, String> = gson.fromJson(back, type)
                for (key in map.keys){
                    location[key] = back.get(key).asDouble
                }
            }
        } catch (e:Exception){
            println(e)
        }
        val worldLevel = if (Level.config.getBoolean("shadowLevels.enable")){
            try {
                val realName = list[0].split("/")[1]
                val uuidString: String? =
                    Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
                        .find(realName)?.value
                //uuidString?.let { Bukkit.getConsoleSender().sendMessage(it) }
                val uuid = UUID.fromString(uuidString)
                val data = ShadowLevelsAPI.getPlayerData(uuid)
                val levelData = data.getLevelData(Level.config.getString("shadowLevels.levelName"))
                if (levelData == null) {
                    Bukkit.getConsoleSender()
                        .sendMessage("§aPixelWorldPro 无法从ShadowLevels内找到等级 ${Level.config.getString("shadowLevels.levelName")}")
                    return null
                }
                levelData.levels.toString()
            }catch (e:Exception){
                list[1]
            }
        }else{
            list[1]
        }
        return WorldData(
            worldName = list[0],
            worldLevel = worldLevel,
            members = members,
            memberName = memberName,
            banPlayers = banPlayers,
            banName = banName,
            state = list[6],
            createTime = list[7],
            lastTime = list[8].toLong(),
            onlinePlayerNumber = list[9].toInt(),
            isCreateNether = list[10].toBoolean(),
            isCreateEnd = list[11].toBoolean(),
            inviter = inviter,
            gameRule = gameRule,
            location = location
        )
    }

    fun serializePlayerData(playerData: PlayerData): String {
        return "${playerData.joinedWorld.joinToString(",")},,|.|," +
                "${playerData.memberNumber},|.|," +
                playerData.inviterMsg.joinToString(",")
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
        val inviter = mutableListOf<UUID>()
        if(list.size >= 3){
            if(list[2] != "") {
                for (id in list[2].split(",")) {
                    inviter.add(UUID.fromString(id))
                }
            }
        }
        return PlayerData(
            joinedWorld = joinedWorld,
            memberNumber = list[1].toInt(),
            inviter
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