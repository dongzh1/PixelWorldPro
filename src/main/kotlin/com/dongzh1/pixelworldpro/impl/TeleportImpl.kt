package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.listener.OnPlayerJoin
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.bungee.server.Bungee
import com.dongzh1.pixelworldpro.bungee.server.Server
import com.dongzh1.pixelworldpro.tools.Serialize
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class TeleportImpl: TeleportApi {

    override fun teleport(uuid: UUID, location: Location, serverName: String?) {
        if (PixelWorldPro.instance.isBungee()) {
            //如果是bungee模式则发送消息到bungee
            if (serverName == null){
                if (Bukkit.getPlayer(uuid) != null){
                    submit {
                        Bukkit.getPlayer(uuid)?.teleport(location)
                    }
                    return
                }
                OnPlayerJoin.add(uuid, location)
                RedisManager.push("teleportUUID|,|$uuid|,|${PixelWorldPro.instance.config.getString("ServerName")}")
                return
            }
            RedisManager.push("teleportLocation|,|${Serialize.serializeLocation(location)}|,|$serverName|,|$uuid")
        }else{
            //如果是本地模式则直接传送
            submit {
                Bukkit.getPlayer(uuid)?.teleport(location)
            }

        }
    }

    override fun teleport(uuid: UUID) {
        teleport(uuid,uuid)
    }

    override fun teleport(uuid: UUID, playerUuid: UUID) {
        submit {
            val worldData = PixelWorldPro.databaseApi.getWorldData(playerUuid) ?: return@submit
            if (PixelWorldPro.instance.isBungee()) {
                //如果是bungee模式则发送消息到bungee
                if (RedisManager.isLock(playerUuid)){
                    //世界已经加载，告诉其他服,uuid要传送到有playeruuid世界的服务器，
                    RedisManager.push("teleportWorld|,|${uuid}|,|${worldData.worldName}/world")
                    val lockValue = RedisManager.getLock()!!.split(",")
                    try {
                        for (value in lockValue) {
                            val list = value.split(":")
                            if (list[0] == playerUuid.toString()) {
                                Bungee.connect(Bukkit.getPlayer(uuid)!!, list[1])
                                if (list[1] == Server.getLocalServer().realName){
                                    val world = Bukkit.getWorld("${worldData.worldName}/world")?:return@submit
                                    val player = Bukkit.getPlayer(uuid)?:return@submit
                                    player.teleport(world.spawnLocation)
                                }
                            }
                        }
                    }catch (_:Exception){}
                }else{
                    //加载世界再传送玩家
                    WorldImpl.loadWorldGroupTp(playerUuid,uuid)
                }
            }else{
                //如果是本地模式则直接传送
                val player = Bukkit.getPlayer(uuid) ?: return@submit
                var world = Bukkit.getWorld(worldData.worldName+"/world")
                if (world == null) {
                    WorldImpl.loadWorldLocal(playerUuid)
                    world = Bukkit.getWorld(worldData.worldName+"/world")
                    if (world == null) {
                        return@submit
                    }else{
                        player.teleport(world.spawnLocation)
                        return@submit
                    }
                }else{
                    player.teleport(world.spawnLocation)
                    return@submit
                }
            }
        }
    }

    override fun teleportDimension(uuid: UUID, playerUuid: UUID, dimension: String) {
        submit {
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return@submit
            //如果是本地模式则直接传送
            val player = Bukkit.getPlayer(playerUuid) ?: return@submit
            var world = Bukkit.getWorld("./${worldData.worldName}/$dimension")
            if (world == null) {
                val back = WorldImpl.loadDimension(uuid, Bukkit.getPlayer(playerUuid)!!, dimension)
                if (back) {
                    world = Bukkit.getWorld("./${worldData.worldName}/$dimension")
                    if (world == null) {
                        return@submit
                    } else {
                        player.sendMessage("传送维度")
                        player.teleport(world.spawnLocation)
                        return@submit
                    }
                } else {
                    player.sendMessage("此世界的${dimension}维度尚未购买/加载失败/没有该维度")
                }
            } else {
                player.teleport(world.spawnLocation)
                return@submit
            }

        }
    }
    fun connect(player: Player, server: String) {
        val byteArray = ByteArrayOutputStream()
        val out = DataOutputStream(byteArray)
        try {
            out.writeUTF("Connect")
            out.writeUTF(server)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        player.sendPluginMessage(PixelWorldPro.instance, "BungeeCord", byteArray.toByteArray())
    }
}