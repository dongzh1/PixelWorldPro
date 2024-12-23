package com.dongzh1.pixelworldpro.bungee.redis

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.PixelWorldPro.Companion.channel
import com.dongzh1.pixelworldpro.bungee.server.Server
import com.dongzh1.pixelworldpro.bungee.world.World
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.tools.Serialize
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.utils.Module
import java.util.*
import kotlin.collections.ArrayList


object RedisManager : Module<EasyPlugin> {

    private val jedisPool = PixelWorldPro.jedisPool

    operator fun set(uuid: UUID, worldData: String) {
        jedisPool.resource.also {
            it.del("PixelWorldPro${uuid}")
            it.save()
            it.set("PixelWorldPro${uuid}", worldData)
            it.save()
            it.close()
        }
    }

    operator fun get(uuid: UUID): WorldData? {
        jedisPool.resource.also {
            val value = it.get("PixelWorldPro${uuid}")
            it.close()
            if (value == null) {
                return null
            }
            return Serialize.deserialize(value)
        }
    }

    fun getWorldDataMap(): MutableMap<UUID, WorldData> {
        val map = mutableMapOf<UUID, WorldData>()
        jedisPool.resource.also {
            val keys = it.keys("PixelWorldPro*-*-*-*-*")
            for (key in keys) {
                val value = it.get(key) ?: continue
                val mapKey = key.replace("PixelWorldPro", "")
                val worldData = Serialize.deserialize(value)!!
                map[UUID.fromString(mapKey)] = worldData
            }
            it.close()
        }
        //排序,根据worldData的onlinePlayer
        if (map.isEmpty()) {
            return map
        }
        return map.toList().sortedByDescending { (_, value) -> value.onlinePlayerNumber }
            .toMap() as MutableMap<UUID, WorldData>
    }
    fun getWorldList(): MutableList<UUID> {
        val list = mutableListOf<UUID>()
        jedisPool.resource.also {
            val keys = it.keys("PixelWorldPro*-*-*-*-*")
            for (key in keys) {
                val mapKey = key.replace("PixelWorldPro", "")
                list.add(UUID.fromString(mapKey))
            }
            it.close()
        }
        return list

    }


    fun remove(uuid: UUID) {
        jedisPool.resource.also {
            it.del("PixelWorldPro${uuid}")
            it.close()
        }
    }

    fun setSeed(uuid: UUID, seed:String){
        push("setSeed|,|${uuid}|,|${seed}")
    }

    fun setLock(uuid: UUID) {
        val lockValue = getLock()
        if (lockValue == null){
            jedisPool.resource.also {
                it.set("PixelWorldProlock", "${uuid}:${World.bungeeConfig.getString("realName")},")
                it.close()
            }
        }else{
            if (lockValue.contains("$uuid")){
                return
            }
            jedisPool.resource.also {
                it.set("PixelWorldProlock", "${lockValue}${uuid}:${World.bungeeConfig.getString("realName")},")
                it.close()
            }
        }
    }

    fun isLock(uuid: UUID): Boolean {
        val lockValue = getLock()
        return lockValue?.contains("$uuid") ?: false
    }
    fun getLock(): String?{
        jedisPool.resource.also {
            val value = it.get("PixelWorldProlock")
            it.close()
            return value
        }
    }


    fun removeLock(uuid: UUID) {
        val lockValue = getLock()
        if (lockValue == null){
            return
        }else{
            if (lockValue.contains("$uuid")){
                jedisPool.resource.also {
                    it.set("PixelWorldProlock", lockValue.replace("${uuid}:${PixelWorldPro.instance.config.getString("ServerName")},",""))
                    it.close()
                }
            }
        }
    }

    fun getLocks(): ArrayList<UUID> {
        val locks = ArrayList<UUID>()
        val lockValue = getLock()
        if (lockValue == null){
            return locks
        }else {
            for (lock in lockValue.split(",")) {
                if (lock.contains(":")) {
                    try {
                        locks.add(UUID.fromString(lock.split(":")[0]))
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return locks
    }

    private fun removeLock(serverName: String){
        var lockValue = getLock()
        if (lockValue == null){
            return
        }else{
            if (lockValue.contains("${serverName},")){
                for (lock in lockValue!!.split(",")){
                    if (lock.contains(":")){
                        if (lock.split(":")[1] == serverName){
                            lockValue = lockValue.replace("$lock,","")
                        }
                    }
                }
                jedisPool.resource.also {
                    it.set("PixelWorldProlock", lockValue)
                    it.close()
                }
            }
        }
    }

    fun push(message: String) {
        jedisPool.resource.use { jedis -> jedis.publish(channel, message) }
    }

    fun getAllWordData(): Map<UUID,String>{
        jedisPool.resource.also {
            //key匹配UUID格式
            val keys = it.keys("PixelWorldPro*-*-*-*-*")
            val map = mutableMapOf<UUID,String>()
            for (key in keys) {
                if (UUID.fromString(key.replace("PixelWorldPro","")) == null) {
                    continue
                }
                map[UUID.fromString(key.replace("PixelWorldPro",""))] = it.get(key)
            }
            it.close()
            return map
        }
    }

    fun test():Boolean{
        jedisPool.resource.also {
            val keys = it.keys("PixelWorldPro*")
            it.close()
            //查询是否有这个key
            return keys.size != 0
        }
    }

    fun closeServer(){
        val serverName = Server.getLocalServer().realName
        removeLock(serverName)
    }
}