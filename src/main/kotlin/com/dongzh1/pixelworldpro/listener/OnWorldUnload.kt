package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import java.util.*

class OnWorldUnload : Listener {
    @EventHandler
    fun worldUnload(e: WorldUnloadEvent) {
        val worldPath = WorldImpl.worldPath()
        if (e.world.name.startsWith(worldPath)) {
            var uuid = e.world.name.substring(worldPath.length + 1)
            uuid = uuid.substring(0, uuid.indexOf("_"))
            if (PixelWorldPro.instance.isBungee()) {
                RedisManager.removeLock(UUID.fromString(uuid))
            }
        }
    }
}