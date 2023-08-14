package com.dongzh1.pixelworldpro.listener


import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.util.*


class OnPlayerJoin : Listener {
    @EventHandler
    fun on(event: PlayerSpawnLocationEvent) {
        event.player.addAttachment(PixelWorldPro.instance)
        if (map.containsKey(event.player.uniqueId)) {
            event.spawnLocation = map[event.player.uniqueId]!!
            map.remove(event.player.uniqueId)
        }
    }

    @EventHandler
    fun login(event: PlayerJoinEvent){
        val playerData = PixelWorldPro.databaseApi.getPlayerData(event.player.uniqueId) ?: return
        if(playerData.inviterMsg.isNotEmpty()){
            val inviterMsg = playerData.inviterMsg as ArrayList
            playerData.inviterMsg = listOf()
            PixelWorldPro.databaseApi.setPlayerData(event.player.uniqueId, playerData)
            Bukkit.getConsoleSender().sendMessage("您收到了${inviterMsg.size}个邀请函")
        }
    }
    companion object {
        private val map: MutableMap<UUID, Location> = HashMap()

        fun add(uuid: UUID, location: Location) {
            map[uuid] = location
            submit(delay = 20 * 60){
                map.remove(uuid)
            }
        }
    }
}