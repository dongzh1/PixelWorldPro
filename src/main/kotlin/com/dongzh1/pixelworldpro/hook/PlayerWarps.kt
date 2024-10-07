package com.dongzh1.pixelworldpro.hook

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.bungee.server.Bungee
import com.dongzh1.pixelworldpro.bungee.server.Server
import com.dongzh1.pixelworldpro.bungee.world.World
import com.dongzh1.pixelworldpro.hook.Dough.getUUID
import com.dongzh1.pixelworldpro.hook.Dough.isPlayerWorld
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.olziedev.playerwarps.api.PlayerWarpsAPI
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent
import com.olziedev.playerwarps.api.events.warp.PlayerWarpRemoveEvent
import com.olziedev.playerwarps.api.events.warp.PlayerWarpTeleportEvent
import com.olziedev.playerwarps.api.warp.WLocation
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import java.io.File


class PlayerWarps: Listener {
    val api: PlayerWarpsAPI = PlayerWarpsAPI.getInstance()

    /*
    @EventHandler
    fun onWarpCreate(event: PlayerWarpCreateEvent) {
        val creator = event.creator as Player
        val worldName = event.playerWarp.warpLocation.world
        if (creator.isOp) {
            val warp = event.playerWarp
            val data = getWorldPlayerWarpsConfig(worldName)
            val warps = data.getLongList("warps")
            warps.add(warp.id)
            data.set("warps", warps)
            data.save(File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "PlayerWarps.yml"))
            return
        }
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, creator.uniqueId)) {
            val warp = event.playerWarp
            val data = getWorldPlayerWarpsConfig(worldName)
            val warps = data.getLongList("warps")
            warps.add(warp.id)
            data.set("warps", warps)
            data.save(File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "PlayerWarps.yml"))
            return
        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(creator.uniqueId)) {
            val warp = event.playerWarp
            val data = getWorldPlayerWarpsConfig(worldName)
            val warps = data.getLongList("warps")
            warps.add(warp.id)
            data.set("warps", warps)
            data.save(File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "PlayerWarps.yml"))
            return
        }
        event.isCancelled = true
    }

    @EventHandler
    fun onWarpRemove(event: PlayerWarpRemoveEvent) {
        val worldName = event.playerWarp.warpLocation.world
        //如果玩家不是成员，则取消事件
        val warp = event.playerWarp
        val data = getWorldPlayerWarpsConfig(worldName)
        val warps = data.getLongList("warps")
        warps.add(warp.id)
        data.set("warps", warps)
        data.save(File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "PlayerWarps.yml"))
        return
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        val uuid = getUUID(event.world.name) ?: return
        val data = getWorldPlayerWarpsConfig(event.world.name)
        val warps = data.getLongList("warps")
        for (id in warps) {
            val warp = api.getSponsorWarp(id.toString(), Bukkit.getConsoleSender())
            val location = warp.warpLocation as WLocation
        }
    }
     */

    @EventHandler
    fun onTeleport(event: PlayerWarpTeleportEvent) {
        val player = event.teleporter
        val worldName = event.playerWarp.warpLocation.world
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, player.uniqueId)) {
            if (PixelWorldPro.instance.isBungee()) {
                World.loadBungeeWorld(player, player.uniqueId)
            } else {
                WorldImpl.loadWorld(player.uniqueId, null)
            }
            return
        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        if (player.isOp) {
            if (PixelWorldPro.instance.isBungee()) {
                World.loadBungeeWorld(player, getUUID(worldName)!!)
            } else {
                WorldImpl.loadWorld(getUUID(worldName)!!, null)
            }
            return
        }
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(player.uniqueId)) {
            if (PixelWorldPro.instance.isBungee()) {
                World.loadBungeeWorld(player, getUUID(worldName)!!)
            } else {
                WorldImpl.loadWorld(getUUID(worldName)!!, null)
            }
            return
        }
        event.isCancelled = true
    }

    private fun getWorldPlayerWarpsConfig(worldName: String):YamlConfiguration {
        val config = File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "PlayerWarps.yml")
        val data = YamlConfiguration()
        if (!config.exists()) {
            config.createNewFile()
        }
        data.load(config)
        return data
    }
}