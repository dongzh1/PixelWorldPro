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

    private val onlineServer = mutableListOf(PixelWorldPro.instance.config.getString("ServerName")!!)
    private val newOnlineServer = mutableListOf(PixelWorldPro.instance.config.getString("ServerName")!!)
    private val push = submit(async = true, period = 20L) {
        RedisManager.push("ServerOnline|,|${PixelWorldPro.instance.config.getString("ServerName")}")
    }
    private val checkServer = submit(async = true, period = 60L, delay = 60L) {
        //如果onlineServer没有数据库里的名字就删除
        if (newOnlineServer.size < onlineServer.size){
            for (server in onlineServer){
                if (!newOnlineServer.contains(server)){
                    RedisManager.closeServer(server)
                }
            }
        }
        newOnlineServer.clear()
    }
    init {
        submit(async = true, delay = 50L) {
            if (onlineServer.size == 1 && onlineServer[0] == PixelWorldPro.instance.config.getString("ServerName")){
                RedisManager.removeMspt()
                RedisManager.removeLock()
                RedisManager.setMspt(100.0)
            }
        }
    }
    fun stop(){
        push.cancel()
        checkServer.cancel()
    }

    fun getOnlineServer(): List<String> {
        return onlineServer
    }

    override fun onMessage(channel: String?, message: String?) {
        if (channel == PixelWorldPro.channel) {
            when(message!!.split("|,|")[0]){
                "ServerOnline" ->{
                    val serverName = message.split("|,|")[1]
                    if (!newOnlineServer.contains(serverName)){
                        newOnlineServer.add(serverName)
                    }
                    if (newOnlineServer.size > onlineServer.size){
                        onlineServer.clear()
                        onlineServer.addAll(newOnlineServer)
                    }
                }
                "createWorld" ->{
                    //获取最低mspt服务器
                    val serverName = TickListener.getLowestMsptServer()
                    //如果当前服务器为最低mspt服务器则创建世界
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val template = message.split("|,|")[2]
                    submit {
                        if (WorldImpl.createWorldLocal(uuid, template)){
                            RedisManager.push("createWorldSuccess|,|${uuid}")
                        }
                    }

                }
                "createWorldSuccess" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    if (WorldImpl.getCreateWorldList().contains(uuid)){
                        WorldImpl.removeCreateWorldList(uuid)
                    }
                }
                "loadWorldGroup" ->{
                    //获取最低mspt服务器
                    val serverName = message.split("|,|")[2]
                    //如果当前服务器为最低mspt服务器则加载世界
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        WorldImpl.loadWorldLocal(uuid)
                    }

                }
                "loadWorldGroupTp" -> {
                    val serverName = message.split("|,|")[3]
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        if (WorldImpl.loadWorldLocal(uuid)){
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
                        if (WorldImpl.loadWorldLocal(uuid)){
                            RedisManager.push("loadWorldSuccess|,|${uuid}")
                        }
                    }
                }
                "loadWorldSuccess" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    if (WorldImpl.getLoadWorldList().contains(uuid)){
                        WorldImpl.removeLoadWorldList(uuid)
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
                "updateWorldLevel" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val level = message.split("|,|")[2]
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    val world = Bukkit.getWorld(worldData!!.worldName)
                    if (world == null){
                        return
                    }else{
                        WorldImpl.setWorldBorder(world,level)
                    }
                }
                "unloadWorld" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    val world = Bukkit.getWorld(worldData!!.worldName)
                    if (world == null){
                        return
                    }else{
                        if (WorldImpl.unloadWorld(world)){
                            RedisManager.push("unloadWorldBack|,|${uuid}")
                        }
                    }
                }
                "unloadWorldBack" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    //获取要卸载是世界uuid，并移除
                    if (WorldImpl.getUnloadWorldList().contains(uuid)){
                        WorldImpl.removeUnloadWorldList(uuid)
                    }
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