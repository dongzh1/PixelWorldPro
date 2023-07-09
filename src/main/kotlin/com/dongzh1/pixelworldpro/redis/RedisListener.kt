@file:Suppress("SameParameterValue")

package com.dongzh1.pixelworldpro.redis

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.impl.TeleportImpl
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.listener.TickListener
import com.dongzh1.pixelworldpro.tools.Serialize
import com.xbaimiao.easylib.module.utils.submit

import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub
import java.util.UUID

class RedisListener : JedisPubSub() {

    override fun onMessage(channel: String?, message: String?) {
        if (channel == PixelWorldPro.channel) {
            when(message!!.split("|,|")[0]){
                "createWorld" ->{
                    //获取最低mspt服务器
                    val serverName = TickListener.getLowestMsptServer()
                    //如果当前服务器为最低mspt服务器则创建世界
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val template = message.split("|,|")[2]
                    WorldImpl().createWorldLocal(uuid, template)
                }
                "loadWorldGroup" ->{
                    //获取最低mspt服务器
                    val serverName = TickListener.getLowestMsptServer()
                    //如果当前服务器为最低mspt服务器则加载世界
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        WorldImpl().loadWorldLocal(uuid)
                    }

                }
                "loadWorldGroupTp" -> {
                    val serverName = TickListener.getLowestMsptServer()
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        if (WorldImpl().loadWorldLocal(uuid)){
                            val playerUuid = UUID.fromString(message.split("|,|")[2])
                            TeleportApi.Factory.teleportApi!!.teleport(playerUuid,uuid)
                        }
                    }
                }
                "loadWorldServer" ->{
                    val serverName = message.split("|,|")[2]
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        WorldImpl().loadWorldLocal(uuid)
                    }
                }
                "teleportUUID" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val serverName = message.split("|,|")[2]
                    if (Bukkit.getPlayer(uuid) == null){
                        return
                    }
                    TeleportImpl().connect(Bukkit.getPlayer(uuid)!!,serverName)
                }
                "teleportLocation" ->{
                    val location = Serialize.deserializeLocation(message.split("|,|")[1])
                    val serverName = message.split("|,|")[2]
                    val uuid = UUID.fromString(message.split("|,|")[3])
                    if (serverName == PixelWorldPro.instance.config.getString("ServerName")){
                        TeleportApi.Factory.teleportApi!!.teleport(uuid,location)
                    }
                }
                "teleportWorld" ->{
                    //找到加载了对应世界的服务器
                    val world = Bukkit.getWorld(message.split("|,|")[2]) ?: return
                    //让玩家传送过来
                    TeleportApi.Factory.teleportApi!!.teleport(UUID.fromString(message.split("|,|")[1]),world.spawnLocation)
                }
                "sendMessage" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val msg = message.split("|,|")[2]
                    if (Bukkit.getPlayer(uuid) == null){
                        return
                    }
                    Bukkit.getPlayer(uuid)!!.sendMessage(msg)
                }
                "setTime" ->{
                    val serverName = message.split("|,|")[1]
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val timeWorld = Bukkit.getWorld(message.split("|,|")[2])
                    if (timeWorld == null){
                        Bukkit.getConsoleSender().sendMessage(lang("WorldTimeWorldNotExist"))
                        return
                    }
                    RedisManager.push("setTimeBack|,|${timeWorld.time}|,|${message.split("|,|")[3]}")
                }
                "setTimeBack" ->{
                    val time = message.split("|,|")[1].toLong()
                    val worldName = message.split("|,|")[2]
                    val world = Bukkit.getWorld(worldName)
                    if (world == null){
                        Bukkit.getConsoleSender().sendMessage(lang("WorldTimeWorldNotExist"))
                        return
                    }
                    world.time = time
                }
                else ->{
                    Bukkit.getLogger().warning("未知的redis消息类型")
                }
            }
        }
    }
    private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}