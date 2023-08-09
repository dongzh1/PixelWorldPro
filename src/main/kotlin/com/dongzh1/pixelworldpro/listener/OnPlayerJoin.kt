package com.dongzh1.pixelworldpro.listener

import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit


import org.bukkit.Location

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

import org.spigotmc.event.player.PlayerSpawnLocationEvent

import java.util.UUID

class OnPlayerJoin : Listener {
    @EventHandler
    fun on(event: PlayerSpawnLocationEvent) {
        if (map.containsKey(event.player.uniqueId)) {
            event.spawnLocation = map[event.player.uniqueId]!!
            map.remove(event.player.uniqueId)
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