package com.dongzh1.pixelworldpro.redis

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.PixelWorldPro.Companion.channel
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.tools.Serialize
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.utils.Module
import org.bukkit.Bukkit
import java.util.*


object RedisManager : Module<EasyPlugin> {

    private val jedisPool = PixelWorldPro.jedisPool

    operator fun set(uuid: UUID, worldData: String) {
        jedisPool.resource.also {
            it.set("PixelWorldPro${uuid}", worldData)
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


    fun remove(uuid: UUID) {
        jedisPool.resource.also {
            it.del("PixelWorldPro${uuid}")
            it.close()
        }
    }

    fun setMspt(mspt: Double) {
        var value = getMspt()
        if (value != null) {
            var isFound = false
            for (server in value!!.split(",")) {
                if (server.split(":")[0] == PixelWorldPro.instance.config.getString("ServerName")) {
                    value = value.replace(server, "${PixelWorldPro.instance.config.getString("ServerName")}:$mspt")
                    isFound = true
                    break
                }
            }
            if (!isFound) {
                value += "${PixelWorldPro.instance.config.getString("ServerName")}:$mspt,"
            }
        } else {
            value = "${PixelWorldPro.instance.config.getString("ServerName")}:$mspt,"
        }

        jedisPool.resource.also {
            it.set("PixelWorldPromspt", value)
            it.close()
        }
    }

    fun getMspt(): String? {
        jedisPool.resource.also {
            val value = it.get("PixelWorldPromspt")
            it.close()
            if (value == null) {
                return null
            }
            return value
        }
    }

    fun setLock(uuid: UUID) {
        val lockValue = getLock()
        if (lockValue == null){
            jedisPool.resource.also {
                it.set("PixelWorldProlock", "${uuid},")
                it.close()
            }
        }else{
            if (lockValue.contains("${uuid},")){
                return
            }
            jedisPool.resource.also {
                it.set("PixelWorldProlock", "${lockValue}${uuid},")
                it.close()
            }
        }
    }

    fun isLock(uuid: UUID): Boolean {
        val lockValue = getLock()
        return lockValue?.contains("$uuid") ?: false
    }
    private fun getLock(): String?{
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
            if (lockValue.contains("${uuid},")){
                jedisPool.resource.also {
                    it.set("PixelWorldProlock", lockValue.replace("${uuid},",""))
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
        //删除本服mspt
        var msptValue = getMspt()
        if (msptValue != null){
            for (server in msptValue!!.split(",")){
                if (server.contains(":")){
                    if (server.split(":")[0] != PixelWorldPro.instance.config.getString("ServerName")){
                        msptValue = msptValue.replace("$server,","")
                        break
                    }
                }
            }
            jedisPool.resource.also {
                it.set("PixelWorldPromspt", msptValue)
                it.close()
            }
        }
        //删除本服所有加载的世界lock
        Bukkit.getWorlds().forEach {
            val worldPath = WorldImpl().worldPath()
            if (it.name.startsWith(worldPath)) {
                var uuid = it.name.substring(worldPath.length + 1)
                uuid = uuid.substring(0, uuid.indexOf("_"))
                removeLock(UUID.fromString(uuid))
            }
        }
    }
}