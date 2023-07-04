package com.dongzh1.pixelworldpro.redis

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldData
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.utils.Module
import org.bukkit.Bukkit
import java.util.*


object RedisManager : Module<EasyPlugin> {

    private val jedisPool = PixelWorldPro.jedisPool

    operator fun set(uuid: UUID, worldData: String) {
        jedisPool.resource.also {
            it.set(uuid.toString(), worldData)
            it.close()
        }
    }

    operator fun get(uuid: UUID): WorldData? {
        jedisPool.resource.also {
            val value = it.get(uuid.toString())
            it.close()
            if (value == null) {
                return null
            }
            return deserialize(value)
        }
    }

    //序列化存入的数据
    fun serialize(worldData: WorldData): String {
        return "${worldData.worldName},|.|," +
                "${worldData.worldShowName},|.|," +
                "${worldData.worldLevel},|.|," +
                "${worldData.members.joinToString(",")},|.|," +
                "${worldData.banPlayers.joinToString(",")},|.|," +
                "${worldData.joinedWorld.joinToString(",")},|.|," +
                "${worldData.state},|.|," +
                "${worldData.createTime},|.|," +
                "${worldData.lastTime},|.|," +
                "${worldData.onlinePlayerNumber},|.|," +
                "${worldData.isCreateNether},|.|," +
                "${worldData.isCreateEnd},|.|," +
                "${worldData.memberNumber}"

    }
    fun deserialize(value: String): WorldData {
        val list = value.split(",|.|,")
        return WorldData(
            list[0],
            list[1],
            list[2],
            list[3].split(",").map { UUID.fromString(it) },
            list[4].split(",").map { UUID.fromString(it) },
            list[5].split(",").map { UUID.fromString(it) },
            list[6],
            list[7],
            list[8].toLong(),
            list[9].toInt(),
            list[10].toBoolean(),
            list[11].toBoolean(),
            list[12].toInt()
        )
    }

}